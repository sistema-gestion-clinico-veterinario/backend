package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.enums.EstadoAccesoPortalPropietario;
import veterinaria.vargasvet.domain.enums.ModoAccesoPropietario;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalPropietarioPolicyServiceTest {

    @Mock ApoderadoRepository apoderadoRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SessionSecurityService sessionSecurityService;

    @InjectMocks PortalPropietarioPolicyService service;

    @Test
    void gestionInternaRevocaElPortalDeLaEmpresaYDevuelveCuantosPropietariosAfecto() {
        Company company = company();
        when(apoderadoRepository.cambiarEstadoAccesoPortalDeLaEmpresa(eq(3), eq(EstadoAccesoPortalPropietario.REVOCADO),
                any(LocalDateTime.class), eq("admin@clinica.test"))).thenReturn(42);

        int afectados;
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("admin@clinica.test");
            afectados = service.aplicarCambio(company, ModoAccesoPropietario.PORTAL_OPCIONAL,
                    ModoAccesoPropietario.SOLO_GESTION_INTERNA);
        }

        assertThat(afectados).isEqualTo(42);
    }

    @Test
    void gestionInternaCierraSesionesBorraRecuperacionesYLimpiaInvitacionesDeLaEmpresa() {
        Company company = company();

        service.aplicarCambio(company, ModoAccesoPropietario.PORTAL_POR_DEFECTO,
                ModoAccesoPropietario.SOLO_GESTION_INTERNA);

        InOrder orden = inOrder(apoderadoRepository, sessionSecurityService, passwordResetTokenRepository, usuarioRepository);
        orden.verify(apoderadoRepository).cambiarEstadoAccesoPortalDeLaEmpresa(eq(3),
                eq(EstadoAccesoPortalPropietario.REVOCADO), any(LocalDateTime.class), any());
        orden.verify(sessionSecurityService).invalidateClientSessionsForCompany(company);
        orden.verify(passwordResetTokenRepository).deleteDeLosApoderadosDeLaEmpresa(3);
        orden.verify(usuarioRepository).limpiarTokenDeVerificacionDeApoderadosSinRol(3, RoleScope.STAFF);
    }

    @Test
    void laFechaDeLaRevocacionEsLaDelMomentoDelCambio() {
        LocalDateTime antes = AppClock.now().minusSeconds(5);
        ArgumentCaptor<LocalDateTime> cuando = ArgumentCaptor.forClass(LocalDateTime.class);

        service.aplicarCambio(company(), ModoAccesoPropietario.PORTAL_OPCIONAL,
                ModoAccesoPropietario.SOLO_GESTION_INTERNA);

        verify(apoderadoRepository).cambiarEstadoAccesoPortalDeLaEmpresa(eq(3), any(), cuando.capture(), any());
        assertThat(cuando.getValue()).isBetween(antes, AppClock.now().plusSeconds(5));
    }

    @Test
    void volverAHabilitarElPortalNoReactivaPropietariosAutomaticamente() {
        int afectados = service.aplicarCambio(company(), ModoAccesoPropietario.SOLO_GESTION_INTERNA,
                ModoAccesoPropietario.PORTAL_OPCIONAL);

        assertThat(afectados).isZero();
        verifyNoInteractions(apoderadoRepository, passwordResetTokenRepository, usuarioRepository,
                sessionSecurityService);
    }

    @Test
    void sinCambioDeModoNoSeTocaNada() {
        int afectados = service.aplicarCambio(company(), ModoAccesoPropietario.SOLO_GESTION_INTERNA,
                ModoAccesoPropietario.SOLO_GESTION_INTERNA);

        assertThat(afectados).isZero();
        verifyNoInteractions(apoderadoRepository, passwordResetTokenRepository, usuarioRepository,
                sessionSecurityService);
    }

    @Test
    void pasarEntreModosConPortalNoRevocaNada() {
        int afectados = service.aplicarCambio(company(), ModoAccesoPropietario.PORTAL_OPCIONAL,
                ModoAccesoPropietario.PORTAL_POR_DEFECTO);

        assertThat(afectados).isZero();
        verifyNoInteractions(apoderadoRepository, passwordResetTokenRepository, usuarioRepository,
                sessionSecurityService);
    }

    private Company company() {
        Company company = new Company();
        company.setId(3);
        company.setName("Clínica Test");
        company.setActivo(true);
        return company;
    }
}
