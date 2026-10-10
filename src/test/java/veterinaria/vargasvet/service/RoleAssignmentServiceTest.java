package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock private UsuarioPorRolRepository assignmentRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AccesoValidator accesoValidator;
    @Mock private AdministratorProtection administratorProtection;
    @Mock private AuditLogService auditLogService;

    private RoleAssignmentService service;
    private Usuario usuario;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new RoleAssignmentService(
                assignmentRepository, roleRepository, accesoValidator,
                administratorProtection, auditLogService);
        usuario = new Usuario();
        usuario.setId(20);
        company = company(7);
    }

    @Test
    void noReescribeNiAuditaCuandoLaAsignacionNoCambio() {
        Role actual = role(3, company, RoleScope.STAFF, RolePurpose.CUSTOM, true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF))
                .thenReturn(List.of(assignment(actual)));

        withCompanyContext(7, () -> service.replaceStaffRoles(usuario, company, Set.of(3)));

        verifyNoInteractions(roleRepository, accesoValidator, administratorProtection, auditLogService);
        verify(assignmentRepository, never()).deleteByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF);
        verify(assignmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reemplazaSoloRolesStaffDeLaEmpresaYRegistraLaAuditoria() {
        Role anterior = role(3, company, RoleScope.STAFF, RolePurpose.CUSTOM, true);
        Role nuevo = role(4, company, RoleScope.STAFF, RolePurpose.CUSTOM, true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF))
                .thenReturn(List.of(assignment(anterior)));
        when(roleRepository.findAllById(Set.of(4))).thenReturn(List.of(nuevo));

        withCompanyContext(7, () -> service.replaceStaffRoles(usuario, company, Set.of(4)));

        verify(accesoValidator).validarModificar("VISTA_ROLES");
        verify(assignmentRepository).deleteByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF);
        ArgumentCaptor<UsuarioPorRol> captor = ArgumentCaptor.forClass(UsuarioPorRol.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getCompany()).isSameAs(company);
        assertThat(captor.getValue().getRol()).isSameAs(nuevo);
        verify(auditLogService).log(
                7, "MODIFICAR_ASIGNACIONES_ROL", "Roles", "Usuario 20: roles [ROL_3] -> [ROL_4]");
    }

    @Test
    void rechazaUnRolDeOtraEmpresaAntesDeModificarAsignaciones() {
        Role ajeno = role(4, company(8), RoleScope.STAFF, RolePurpose.CUSTOM, true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF))
                .thenReturn(List.of());
        when(roleRepository.findAllById(Set.of(4))).thenReturn(List.of(ajeno));

        assertThatThrownBy(() -> withCompanyContext(
                7, () -> service.replaceStaffRoles(usuario, company, Set.of(4))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("empresa activa");

        verify(assignmentRepository, never()).deleteByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF);
        verify(assignmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retirarAdministradorProtegeQueLaEmpresaConserveOtroAdministrador() {
        Role admin = role(1, null, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, true);
        Role veterinario = role(2, company, RoleScope.STAFF, RolePurpose.CUSTOM, true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF))
                .thenReturn(List.of(assignment(admin)));
        when(roleRepository.findAllById(Set.of(2))).thenReturn(List.of(veterinario));

        withCompanyContext(7, () -> service.replaceStaffRoles(usuario, company, Set.of(2)));

        verify(administratorProtection).assertCanRemoveAdministratorRole(usuario, 7);
    }

    @Test
    void altaInicialDeClienteUsaElRolBaseSinExigirAdministrarRoles() {
        Role portal = role(5, company, RoleScope.CLIENT, RolePurpose.CLIENT_PORTAL, true);
        portal.setSystemManaged(true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.CLIENT))
                .thenReturn(List.of());
        when(roleRepository.findAllById(Set.of(5))).thenReturn(List.of(portal));

        withCompanyContext(7, () -> service.replaceClientRoles(usuario, company, Set.of(5)));

        verify(accesoValidator, never()).validarModificar("VISTA_ROLES");
        verify(assignmentRepository).save(org.mockito.ArgumentMatchers.any(UsuarioPorRol.class));
    }

    @Test
    void soloLaPlataformaPuedeConcederAdministradorDeEmpresa() {
        Role admin = role(1, null, RoleScope.STAFF, RolePurpose.COMPANY_ADMIN, true);
        when(assignmentRepository.findByUsuarioIdAndCompanyIdAndRoleScope(20, 7, RoleScope.STAFF))
                .thenReturn(List.of());
        when(roleRepository.findAllById(Set.of(1))).thenReturn(List.of(admin));

        assertThatThrownBy(() -> withCompanyContext(
                7, () -> service.replaceStaffRoles(usuario, company, Set.of(1))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo la plataforma");
    }

    private UsuarioPorRol assignment(Role role) {
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setUsuario(usuario);
        assignment.setCompany(company);
        assignment.setRol(role);
        return assignment;
    }

    private Role role(int id, Company owner, RoleScope scope, RolePurpose purpose, boolean active) {
        Role role = new Role();
        role.setId(id);
        role.setName("ROL_" + id);
        role.setCompany(owner);
        role.setScope(scope);
        role.setPurpose(purpose);
        role.setActivo(active);
        return role;
    }

    private Company company(int id) {
        Company result = new Company();
        result.setId(id);
        result.setName("Empresa " + id);
        return result;
    }

    private void withCompanyContext(int companyId, Runnable action) {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(companyId);
            action.run();
        }
    }
}
