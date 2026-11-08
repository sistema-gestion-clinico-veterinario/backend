package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoAccesoPortalPropietario;
import veterinaria.vargasvet.domain.enums.ModoAccesoPropietario;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortalPropietarioPolicyServiceTest {

    @Mock ApoderadoRepository apoderadoRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SessionSecurityService sessionSecurityService;

    @InjectMocks PortalPropietarioPolicyService service;

    @Test
    void gestionInternaRevocaElAccesoSinEliminarDatosNiLaCuenta() {
        Company company = company();
        Usuario usuario = usuario(9, "propietario@clinica.test");
        usuario.setVerificationToken("invitacion-pendiente");
        usuario.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(2));
        Apoderado apoderado = apoderado(usuario, company);

        when(apoderadoRepository.findByCompanyId(3)).thenReturn(List.of(apoderado));
        when(usuarioPorRolRepository.existsActiveRoleScope(9, 3, RoleScope.STAFF)).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserEmail).thenReturn("admin@clinica.test");
            int afectados = service.aplicarCambio(company, ModoAccesoPropietario.PORTAL_OPCIONAL,
                    ModoAccesoPropietario.SOLO_GESTION_INTERNA);

            assertThat(afectados).isEqualTo(1);
        }

        assertThat(apoderado.getEstadoAccesoPortal()).isEqualTo(EstadoAccesoPortalPropietario.REVOCADO);
        assertThat(apoderado.getEstado()).isTrue();
        assertThat(apoderado.getAccesoPortalModificadoPor()).isEqualTo("admin@clinica.test");
        assertThat(usuario.getVerificationToken()).isNull();
        verify(apoderadoRepository).save(apoderado);
        verify(sessionSecurityService).invalidateSessionsForCompany(usuario, company);
        verify(passwordResetTokenRepository).deleteByUsuarioAndCompany(usuario, company);
        verify(usuarioRepository).save(usuario);
        verify(apoderadoRepository, never()).delete(any());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void unEmpleadoPropietarioConservaSuInvitacionLaboralPeroPierdeElPortal() {
        Company company = company();
        Usuario usuario = usuario(10, "empleado@clinica.test");
        usuario.setVerificationToken("alta-laboral");
        Apoderado apoderado = apoderado(usuario, company);
        when(apoderadoRepository.findByCompanyId(3)).thenReturn(List.of(apoderado));
        when(usuarioPorRolRepository.existsActiveRoleScope(10, 3, RoleScope.STAFF)).thenReturn(true);

        service.aplicarCambio(company, ModoAccesoPropietario.PORTAL_POR_DEFECTO,
                ModoAccesoPropietario.SOLO_GESTION_INTERNA);

        assertThat(apoderado.getEstadoAccesoPortal()).isEqualTo(EstadoAccesoPortalPropietario.REVOCADO);
        assertThat(usuario.getVerificationToken()).isEqualTo("alta-laboral");
        verify(usuarioRepository, never()).save(usuario);
    }

    @Test
    void volverAHabilitarElPortalNoReactivaPropietariosAutomaticamente() {
        int afectados = service.aplicarCambio(company(), ModoAccesoPropietario.SOLO_GESTION_INTERNA,
                ModoAccesoPropietario.PORTAL_OPCIONAL);

        assertThat(afectados).isZero();
        verifyNoInteractions(apoderadoRepository, passwordResetTokenRepository, usuarioPorRolRepository,
                usuarioRepository, sessionSecurityService);
    }

    private Company company() {
        Company company = new Company();
        company.setId(3);
        company.setName("Clínica Test");
        company.setActivo(true);
        return company;
    }

    private Usuario usuario(int id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setActivo(true);
        return usuario;
    }

    private Apoderado apoderado(Usuario usuario, Company company) {
        Apoderado apoderado = new Apoderado();
        apoderado.setId((long) usuario.getId());
        apoderado.setUser(usuario);
        apoderado.setCompany(company);
        apoderado.setEstado(true);
        apoderado.setEstadoAccesoPortal(EstadoAccesoPortalPropietario.ACTIVO);
        return apoderado;
    }
}
