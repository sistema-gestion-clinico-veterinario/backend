package veterinaria.vargasvet.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.security.AccountLockoutService;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.service.impl.AuditLogServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cada evento de auditoría deja constancia del navegador y el sistema desde los que se originó. */
class AuditoriaConDispositivoTest {

    private static final String CHROME_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";
    private static final String SAFARI_IPHONE =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

    private final AuditLogRepository repositorio = mock(AuditLogRepository.class);
    private final AuditLogServiceImpl servicio = new AuditLogServiceImpl(repositorio, mock(AuditRealtimePublisher.class),
            mock(CompanyRepository.class), mock(ClientIpResolver.class));

    private AuditLog guardado() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    private void registrar() {
        when(repositorio.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));
        servicio.log("ana@clinica.test", "ROLE_ADMIN", 7, "Clínica Patitas", "PUBLICAR_AVISO_PRIVACIDAD", "Seguridad",
                "Se publicó la versión 2", "190.40.10.5");
    }

    @Test
    void elEventoGuardaElDispositivoDeQuienHizoLaPeticion() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("User-Agent", CHROME_WINDOWS);
        ReflectionTestUtils.setField(servicio, "httpServletRequest", peticion);

        registrar();

        assertThat(guardado().getDispositivo()).isEqualTo("Chrome en Windows");
    }

    @Test
    void sirveParaCualquierEquipoComoUnCelular() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("User-Agent", SAFARI_IPHONE);
        ReflectionTestUtils.setField(servicio, "httpServletRequest", peticion);

        registrar();

        assertThat(guardado().getDispositivo()).isEqualTo("Safari en iOS");
    }

    @Test
    void sinPeticionComoEnUnProcesoDelSistemaNoSeInventaUnDispositivo() {
        registrar();

        assertThat(guardado().getDispositivo()).isNull();
    }

    @Test
    void siLaPeticionYaNoEstaDisponibleElEventoSeGuardaDeTodosModos() {
        HttpServletRequest sinHilo = mock(HttpServletRequest.class);
        when(sinHilo.getHeader("User-Agent")).thenThrow(new IllegalStateException("No thread-bound request"));
        ReflectionTestUtils.setField(servicio, "httpServletRequest", sinHilo);

        registrar();

        assertThat(guardado().getDispositivo()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void losIntentosDeInicioDeSesionTambienNombranElDispositivo() {
        AuditLogRepository auditoria = mock(AuditLogRepository.class);
        when(auditoria.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));
        ObjectProvider<HttpServletRequest> proveedor = mock(ObjectProvider.class);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("User-Agent", CHROME_WINDOWS);
        when(proveedor.getIfAvailable()).thenReturn(peticion);
        AuthenticationAuditService servicioDeInicio = new AuthenticationAuditService(auditoria,
                mock(AuditRealtimePublisher.class), proveedor, mock(ClientIpResolver.class), mock(AccountLockoutService.class));
        Usuario ana = new Usuario();
        ana.setEmail("ana@clinica.test");

        servicioDeInicio.record(ana, "LOGIN_EXITOSO", "Inicio de sesión");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditoria).save(captor.capture());
        assertThat(captor.getValue().getDispositivo()).isEqualTo("Chrome en Windows");
    }
}
