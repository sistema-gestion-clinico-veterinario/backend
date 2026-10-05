package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Una empresa no puede quedarse sin administración: solo un administrador gestiona la cuenta de
 * otro administrador, y nunca se desactiva al único administrador activo. La plataforma queda
 * fuera de estas reglas porque es quien puede reasignar la administración.
 */
@ExtendWith(MockitoExtension.class)
class AdministratorProtectionTest {

    private static final int COMPANY_ID = 3;

    @Mock UsuarioPorRolRepository usuarioPorRolRepository;
    @Mock CompanyMembershipService companyMembershipService;
    @Mock veterinaria.vargasvet.repository.CompanyRepository companyRepository;
    @InjectMocks AdministratorProtection protection;

    private Company company;
    private Usuario administrador;
    private Usuario otroAdministrador;
    private Usuario empleadoComun;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(COMPANY_ID);
        administrador = usuario(10);
        otroAdministrador = usuario(11);
        empleadoComun = usuario(12);

        lenient().when(usuarioPorRolRepository.findByUsuarioId(10))
                .thenReturn(List.of(asignacion(administrador, RolePurpose.COMPANY_ADMIN, true, company)));
        lenient().when(usuarioPorRolRepository.findByUsuarioId(11))
                .thenReturn(List.of(asignacion(otroAdministrador, RolePurpose.COMPANY_ADMIN, true, company)));
        lenient().when(usuarioPorRolRepository.findByUsuarioId(12))
                .thenReturn(List.of(asignacion(empleadoComun, RolePurpose.CUSTOM, true, company)));
        lenient().when(companyMembershipService.hasActiveStaffMembership(11, COMPANY_ID)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(int id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setActivo(true);
        return usuario;
    }

    private UsuarioPorRol asignacion(Usuario usuario, RolePurpose purpose, boolean activo, Company empresa) {
        Role role = new Role();
        role.setPurpose(purpose);
        role.setActivo(activo);
        role.setScope(RoleScope.STAFF);
        UsuarioPorRol asignacion = new UsuarioPorRol();
        asignacion.setUsuario(usuario);
        asignacion.setRol(role);
        asignacion.setCompany(empresa);
        return asignacion;
    }

    private void actuarComo(RolePurpose purpose) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "actor@empresa.test", "", List.of(), COMPANY_ID,
                2, RoleScope.STAFF, purpose, 0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void reconoceAUnAdministradorSoloDentroDeSuEmpresa() {
        assertThat(protection.isAdministrator(administrador, COMPANY_ID)).isTrue();
        assertThat(protection.isAdministrator(administrador, 99)).isFalse();
        assertThat(protection.isAdministrator(empleadoComun, COMPANY_ID)).isFalse();
    }

    @Test
    void unRolAdministradorDesactivadoNoCuentaComoAdministrador() {
        lenient().when(usuarioPorRolRepository.findByUsuarioId(10))
                .thenReturn(List.of(asignacion(administrador, RolePurpose.COMPANY_ADMIN, false, company)));

        assertThat(protection.isAdministrator(administrador, COMPANY_ID)).isFalse();
    }

    @Test
    void quienNoEsAdministradorNoPuedeGestionarLaCuentaDeUnAdministrador() {
        actuarComo(RolePurpose.CUSTOM);

        assertThatThrownBy(() -> protection.assertCanManage(administrador, COMPANY_ID))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> protection.assertCanDeactivate(administrador, COMPANY_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void quienNoEsAdministradorSiPuedeGestionarAUnEmpleadoComun() {
        actuarComo(RolePurpose.CUSTOM);

        assertThatCode(() -> protection.assertCanDeactivate(empleadoComun, COMPANY_ID)).doesNotThrowAnyException();
    }

    @Test
    void unAdministradorPuedeDesactivarAOtroSiQuedaAlgunAdministradorActivo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatCode(() -> protection.assertCanDeactivate(administrador, COMPANY_ID)).doesNotThrowAnyException();
    }

    @Test
    void nuncaSeDesactivaAlUnicoAdministradorActivo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador));

        assertThatThrownBy(() -> protection.assertCanDeactivate(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");
    }

    @Test
    void unAdministradorPendienteDeActivarONoActivoNoCuentaComoReemplazo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        otroAdministrador.setActivo(false);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatThrownBy(() -> protection.assertCanDeactivate(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unAdministradorSinRelacionActivaConLaEmpresaNoCuentaComoReemplazo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(companyMembershipService.hasActiveStaffMembership(11, COMPANY_ID)).thenReturn(false);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatThrownBy(() -> protection.assertCanDeactivate(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void quitarElRolAdministradorAlUnicoAdministradorSeRechazaConSuPropioMensaje() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador));

        assertThatThrownBy(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quitar el rol de administrador al único administrador activo");
    }

    @Test
    void quitarElRolAdministradorExigeSerAdministrador() {
        actuarComo(RolePurpose.CUSTOM);

        assertThatThrownBy(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void quitarElRolAdministradorConOtroAdministradorActivoEsPosible() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatCode(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void cambiarLosRolesDeUnEmpleadoComunNoConsultaLosAdministradores() {
        actuarComo(RolePurpose.CUSTOM);

        assertThatCode(() -> protection.assertCanRemoveAdministratorRole(empleadoComun, COMPANY_ID))
                .doesNotThrowAnyException();
        org.mockito.Mockito.verify(usuarioPorRolRepository, org.mockito.Mockito.never())
                .findUsersWithActiveRolePurpose(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void losAdministradoresDeOtraEmpresaNoCuentanComoReemplazo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        Usuario adminDeOtraClinica = usuario(30);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador));
        lenient().when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(4, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(adminDeOtraClinica));
        lenient().when(companyMembershipService.hasActiveStaffMembership(30, 4)).thenReturn(true);

        assertThatThrownBy(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laPlataformaPuedeGestionarAdministradoresPeroNoDejarAUnaEmpresaSinNinguno() {
        actuarComo(RolePurpose.PLATFORM_ADMIN);
        assertThatCode(() -> protection.assertCanManage(administrador, COMPANY_ID)).doesNotThrowAnyException();
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador));

        assertThatThrownBy(() -> protection.assertCanDeactivate(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");
        assertThatThrownBy(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laPlataformaPuedeDesactivarAUnAdministradorSiQuedaOtroConPuesto() {
        actuarComo(RolePurpose.PLATFORM_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatCode(() -> protection.assertCanDeactivate(administrador, COMPANY_ID)).doesNotThrowAnyException();
    }

    @Test
    void unAdministradorSuspendidoComoEmpleadoQueSigueSiendoClienteNoCuentaComoReemplazo() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        lenient().when(companyMembershipService.hasActiveMembership(11, COMPANY_ID)).thenReturn(true);
        when(companyMembershipService.hasActiveStaffMembership(11, COMPANY_ID)).thenReturn(false);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        assertThatThrownBy(() -> protection.assertCanRemoveAdministratorRole(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> protection.assertNotLastAdministrator(administrador, COMPANY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("único administrador activo");
    }

    @Test
    void antesDeContarReemplazosSeBloqueaLaFilaDeLaEmpresa() {
        actuarComo(RolePurpose.COMPANY_ADMIN);
        when(usuarioPorRolRepository.findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN))
                .thenReturn(List.of(administrador, otroAdministrador));

        protection.assertCanDeactivate(administrador, COMPANY_ID);

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(companyRepository, usuarioPorRolRepository);
        orden.verify(companyRepository).lockById(COMPANY_ID);
        orden.verify(usuarioPorRolRepository).findUsersWithActiveRolePurpose(COMPANY_ID, RolePurpose.COMPANY_ADMIN);
    }

    @Test
    void sobreUnEmpleadoComunNoSeBloqueaLaEmpresa() {
        actuarComo(RolePurpose.COMPANY_ADMIN);

        protection.assertCanDeactivate(empleadoComun, COMPANY_ID);

        org.mockito.Mockito.verifyNoInteractions(companyRepository);
    }

    @Test
    void desactivarAUnEmpleadoComunNoConsultaLosAdministradores() {
        actuarComo(RolePurpose.COMPANY_ADMIN);

        protection.assertCanDeactivate(empleadoComun, COMPANY_ID);

        org.mockito.Mockito.verify(usuarioPorRolRepository, org.mockito.Mockito.never())
                .findUsersWithActiveRolePurpose(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
