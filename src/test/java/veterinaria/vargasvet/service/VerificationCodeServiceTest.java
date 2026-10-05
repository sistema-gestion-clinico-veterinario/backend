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
import veterinaria.vargasvet.exception.InvalidVerificationCodeException;
import veterinaria.vargasvet.repository.CodigoVerificacionRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.util.AppClock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        verify(repository).deletePendientes(10, 7, "CIERRE_CUENTA");
    }

    @Test
    void unCodigoNuevoReemplazaAlPendiente() {
        service.issue(usuario, company, "CIERRE_CUENTA");

        verify(repository).deletePendientes(10, 7, "CIERRE_CUENTA");
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
}
