package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.CodigoVerificacion;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.exception.CodeResendTooSoonException;
import veterinaria.vargasvet.exception.InvalidVerificationCodeException;
import veterinaria.vargasvet.repository.CodigoVerificacionRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock CodigoVerificacionRepository repository;

    private VerificationCodeService service;
    private Usuario usuario;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new VerificationCodeService(repository);
        ReflectionTestUtils.setField(service, "validityMinutes", 10L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        usuario = new Usuario();
        usuario.setId(10);
        company = new Company();
        company.setId(7);
    }

    private CodigoVerificacion guardado() {
        String code = service.issue(usuario, company, "CIERRE_CUENTA");
        ArgumentCaptor<CodigoVerificacion> row = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(repository).save(row.capture());
        CodigoVerificacion saved = row.getValue();
        saved.setCodigoHash(SecurityTokenUtils.hash(saved.getSal() + ":" + code));
        ReflectionTestUtils.setField(saved, "id", 1L);
        pendiente(saved);
        lastCode = code;
        return saved;
    }

    private String lastCode;

    private void pendiente(CodigoVerificacion row) {
        when(repository.findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(10, 7, "CIERRE_CUENTA"))
                .thenReturn(Optional.of(row));
    }

    @Test
    void elCodigoTiene6DigitosVale10MinutosYSoloSeGuardaSuHashConSal() {
        String code = service.issue(usuario, company, "CIERRE_CUENTA");

        ArgumentCaptor<CodigoVerificacion> row = ArgumentCaptor.forClass(CodigoVerificacion.class);
        verify(repository).save(row.capture());
        CodigoVerificacion saved = row.getValue();
        assertThat(code).matches("\\d{6}");
        assertThat(saved.getCodigoHash()).hasSize(64).doesNotContain(code);
        assertThat(saved.getCodigoHash()).isEqualTo(SecurityTokenUtils.hash(saved.getSal() + ":" + code));
        assertThat(saved.getSal()).hasSize(16);
        assertThat(saved.getExpiraAt()).isBetween(AppClock.now().plusMinutes(9), AppClock.now().plusMinutes(11));
        assertThat(saved.getIntentos()).isZero();
        verify(repository).invalidarPendientes(eq(10), eq(7), eq("CIERRE_CUENTA"), any(LocalDateTime.class));
    }

    @Test
    void unCodigoNuevoReemplazaAlPendiente() {
        service.issue(usuario, company, "CIERRE_CUENTA");

        verify(repository).invalidarPendientes(eq(10), eq(7), eq("CIERRE_CUENTA"), any(LocalDateTime.class));
    }

    @Test
    void elCodigoCorrectoSeConsumeUnaVez() {
        CodigoVerificacion row = guardado();

        service.verifyAndConsume(10, 7, "CIERRE_CUENTA", " " + lastCode + " ");

        assertThat(row.getUsadoAt()).isNotNull();
    }

    @Test
    void unCodigoIncorrectoSumaUnIntentoYSeRechaza() {
        CodigoVerificacion row = guardado();
        String incorrecto = lastCode.equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.verifyAndConsume(10, 7, "CIERRE_CUENTA", incorrecto))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("no es correcto");

        assertThat(row.getIntentos()).isEqualTo(1);
        assertThat(row.getUsadoAt()).isNull();
    }

    @Test
    void trasCincoIntentosFallidosYaNoSirveNiElCodigoCorrecto() {
        CodigoVerificacion row = guardado();
        row.setIntentos(5);

        assertThatThrownBy(() -> service.verifyAndConsume(10, 7, "CIERRE_CUENTA", lastCode))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("Superaste los intentos");

        assertThat(row.getUsadoAt()).isNull();
    }

    @Test
    void unCodigoVencidoSeRechaza() {
        CodigoVerificacion row = guardado();
        row.setExpiraAt(AppClock.now().minusSeconds(1));

        assertThatThrownBy(() -> service.verifyAndConsume(10, 7, "CIERRE_CUENTA", lastCode))
                .isInstanceOf(InvalidVerificationCodeException.class)
                .hasMessageContaining("venció");

        assertThat(row.getUsadoAt()).isNull();
    }

    @Test
    void sinCodigoPendienteSeRechaza() {
        when(repository.findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(10, 7, "CIERRE_CUENTA"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndConsume(10, 7, "CIERRE_CUENTA", "123456"))
                .isInstanceOf(InvalidVerificationCodeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void unCodigoVacioCuentaComoIntentoFallido() {
        CodigoVerificacion row = guardado();

        assertThatThrownBy(() -> service.verifyAndConsume(10, 7, "CIERRE_CUENTA", null))
                .isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(row.getIntentos()).isEqualTo(1);
    }

    @Test
    void elCodigoDeUnaClinicaNoSirveEnOtra() {
        when(repository.findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(10, 8, "CIERRE_CUENTA"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndConsume(10, 8, "CIERRE_CUENTA", "123456"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    private CodigoVerificacion emitidoHace(long segundos) {
        CodigoVerificacion anterior = new CodigoVerificacion();
        anterior.setCreadoAt(AppClock.now().minusSeconds(segundos));
        when(repository.findFirstByUsuarioIdAndCompanyIdAndPropositoOrderByCreadoAtDesc(10, 7, "CIERRE_CUENTA"))
                .thenReturn(Optional.of(anterior));
        return anterior;
    }

    @Test
    void noSePuedePedirOtroCodigoAntesDeLaEsperaMinimaYNoSeTocaElAnterior() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        emitidoHace(20);

        assertThatThrownBy(() -> service.issue(usuario, company, "CIERRE_CUENTA"))
                .isInstanceOfSatisfying(CodeResendTooSoonException.class, ex ->
                        assertThat(ex.getRetryAfterSeconds()).isBetween(38L, 40L))
                .hasMessageContaining("Espera");

        verify(repository, never()).invalidarPendientes(any(), any(), any(), any());
        verify(repository, never()).save(any(CodigoVerificacion.class));
    }

    @Test
    void pasadaLaEsperaMinimaSeEmiteElCodigoNuevoYSeReemplazaAlAnterior() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        emitidoHace(61);

        String code = service.issue(usuario, company, "CIERRE_CUENTA");

        assertThat(code).matches("\\d{6}");
        verify(repository).invalidarPendientes(eq(10), eq(7), eq("CIERRE_CUENTA"), any(LocalDateTime.class));
        verify(repository).save(any(CodigoVerificacion.class));
    }

    @Test
    void elPrimerCodigoSeEmiteSinEspera() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);

        String code = service.issue(usuario, company, "CIERRE_CUENTA");

        assertThat(code).matches("\\d{6}");
    }

    @Test
    void laEsperaMinimaEsPorProposito() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        CodigoVerificacion reciente = new CodigoVerificacion();
        reciente.setCreadoAt(AppClock.now().minusSeconds(5));
        org.mockito.Mockito.lenient()
                .when(repository.findFirstByUsuarioIdAndCompanyIdAndPropositoOrderByCreadoAtDesc(10, 7, "CIERRE_CUENTA"))
                .thenReturn(Optional.of(reciente));

        String otroProposito = service.issue(usuario, company, "OTRO_PROPOSITO");

        assertThat(otroProposito).matches("\\d{6}");
    }

    private void yaSeEmitieron(long cuantos, long hace) {
        emitidoHace(hace);
        hayEnLaVentana(cuantos);
    }

    private void hayEnLaVentana(long cuantos) {
        org.mockito.Mockito.lenient().when(repository.countByUsuarioIdAndCompanyIdAndPropositoAndCreadoAtAfter(eq(10), eq(7), eq("CIERRE_CUENTA"), any(LocalDateTime.class)))
                .thenReturn(cuantos);
    }

    @Test
    void cadaReenvioExigeEsperarElDobleQueElAnterior() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        ReflectionTestUtils.setField(service, "resendMaxSeconds", 900L);

        hayEnLaVentana(1);
        assertThat(service.segundosParaPedirOtro(usuario, company, "CIERRE_CUENTA")).isEqualTo(60);
        hayEnLaVentana(2);
        assertThat(service.segundosParaPedirOtro(usuario, company, "CIERRE_CUENTA")).isEqualTo(120);
        hayEnLaVentana(3);
        assertThat(service.segundosParaPedirOtro(usuario, company, "CIERRE_CUENTA")).isEqualTo(240);
    }

    @Test
    void laEsperaCrecienteTieneUnTope() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        ReflectionTestUtils.setField(service, "resendMaxSeconds", 900L);
        hayEnLaVentana(10);

        assertThat(service.segundosParaPedirOtro(usuario, company, "CIERRE_CUENTA")).isEqualTo(900);
    }

    @Test
    void conDosCodigosYaEmitidosNoBastaConEsperar60Segundos() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        ReflectionTestUtils.setField(service, "resendMaxSeconds", 900L);
        yaSeEmitieron(2, 70);

        assertThatThrownBy(() -> service.issue(usuario, company, "CIERRE_CUENTA"))
                .isInstanceOfSatisfying(CodeResendTooSoonException.class, ex ->
                        assertThat(ex.getRetryAfterSeconds()).isBetween(49L, 51L));
        verify(repository, never()).save(any(CodigoVerificacion.class));
    }

    @Test
    void conDosCodigosYaEmitidosSePermiteTrasLosDosMinutos() {
        ReflectionTestUtils.setField(service, "resendSeconds", 60L);
        ReflectionTestUtils.setField(service, "resendMaxSeconds", 900L);
        yaSeEmitieron(2, 121);

        assertThat(service.issue(usuario, company, "CIERRE_CUENTA")).matches("\\d{6}");
    }
}
