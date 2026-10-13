package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.exception.StalePermissionConfigurationException;
import veterinaria.vargasvet.service.AuditLogService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RolVistaPermisoRepository rolVistaPermisoRepository = mock(RolVistaPermisoRepository.class);
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository = mock(RolVentanaPermisoRepository.class);
    private final UsuarioPorRolRepository usuarioPorRolRepository = mock(UsuarioPorRolRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final VistaRepository vistaRepository = mock(VistaRepository.class);
    private final RoleServiceImpl service = new RoleServiceImpl(
            roleRepository,
            mock(CompanyRepository.class),
            vistaRepository,
            rolVistaPermisoRepository,
            mock(VentanaRepository.class),
            rolVentanaPermisoRepository,
            usuarioPorRolRepository,
            auditLogService
    );

    @Test
    void getAssignableRolesReturnsOnlyActiveRolesFromRequestedScope() {
        Company company = company(7);
        Role activeClient = role(1, company, RoleScope.CLIENT, true);
        Role inactiveClient = role(2, company, RoleScope.CLIENT, false);
        Role activeStaff = role(3, company, RoleScope.STAFF, true);
        when(roleRepository.findByCompanyId(7)).thenReturn(List.of(activeClient, inactiveClient, activeStaff));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var result = service.getAssignableRoles(7, RoleScope.CLIENT);

            assertEquals(List.of(1), result.stream().map(dto -> dto.getId()).toList());
        }
    }

    @Test
    void getAssignableRolesRejectsPlatformScope() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.getAssignableRoles(7, RoleScope.PLATFORM));
        }
    }

    @Test
    void getAssignableRolesRejectsAnotherCompany() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            assertThrows(AccessDeniedException.class,
                    () -> service.getAssignableRoles(8, RoleScope.CLIENT));
        }
    }

    @Test
    void getAssignableStaffRolesIncludesGlobalCompanyAdminForSuperAdmin() {
        Company company = company(7);
        Role customStaff = role(3, company, RoleScope.STAFF, true);
        Role globalAdmin = role(4, null, RoleScope.STAFF, true);
        globalAdmin.setPurpose(RolePurpose.COMPANY_ADMIN);
        when(roleRepository.findByCompanyId(7)).thenReturn(List.of(customStaff));
        when(roleRepository.findFirstByCompanyIsNullAndPurpose(RolePurpose.COMPANY_ADMIN))
                .thenReturn(java.util.Optional.of(globalAdmin));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var result = service.getAssignableRoles(7, RoleScope.STAFF);

            assertEquals(List.of(3, 4), result.stream().map(dto -> dto.getId()).toList());
        }
    }

    @Test
    void getAssignableStaffRolesDoesNotExposeGlobalAdminToCompanyAdmin() {
        Company company = company(7);
        Role customStaff = role(3, company, RoleScope.STAFF, true);
        when(roleRepository.findByCompanyId(7)).thenReturn(List.of(customStaff));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            var result = service.getAssignableRoles(7, RoleScope.STAFF);

            assertEquals(List.of(3), result.stream().map(dto -> dto.getId()).toList());
        }
    }

    @Test
    void saveVistasByRoleRejectsStalePermissionVersionBeforeChangingGrants() {
        Role role = role(12, null, RoleScope.PLATFORM, true);
        role.setPermissionVersion(4L);
        when(roleRepository.findByIdForPermissionUpdate(12)).thenReturn(Optional.of(role));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(StalePermissionConfigurationException.class,
                    () -> service.saveVistasByRole(12, 3L, List.of()));

            verify(rolVistaPermisoRepository, never()).deleteByRolId(12);
        }
    }

    @Test
    void updateRoleAllowsScopeChangeOnAnEmptyRole() {
        Company company = company(7);
        Role role = role(9, company, RoleScope.STAFF, true);
        when(roleRepository.findById(9)).thenReturn(java.util.Optional.of(role));
        when(rolVistaPermisoRepository.findByRolId(9)).thenReturn(List.of());
        when(usuarioPorRolRepository.existsByRolId(9)).thenReturn(false);
        when(roleRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var result = service.updateRole(9, "Asistente Veterinario", null, RoleScope.CLIENT);

            assertEquals(RoleScope.CLIENT, result.getScope());
        }
    }

    @Test
    void updateRoleRejectsScopeChangeWhenRoleHasGrantedPermissions() {
        Company company = company(7);
        Role role = role(9, company, RoleScope.STAFF, true);
        RolVistaPermiso permiso = new RolVistaPermiso();
        permiso.setLeer(true);
        when(roleRepository.findById(9)).thenReturn(java.util.Optional.of(role));
        when(rolVistaPermisoRepository.findByRolId(9)).thenReturn(List.of(permiso));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.updateRole(9, "Asistente Veterinario", null, RoleScope.CLIENT));
        }
    }

    @Test
    void updateRoleRejectsScopeChangeWhenRoleHasAssignedUsers() {
        Company company = company(7);
        Role role = role(9, company, RoleScope.STAFF, true);
        when(roleRepository.findById(9)).thenReturn(java.util.Optional.of(role));
        when(rolVistaPermisoRepository.findByRolId(9)).thenReturn(List.of());
        when(usuarioPorRolRepository.existsByRolId(9)).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.updateRole(9, "Asistente Veterinario", null, RoleScope.CLIENT));
        }
    }

    @Test
    void createRoleRejectsACompanyRoleWithoutCompany() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.createRole("Recepción", null, null, RoleScope.STAFF));
        }
    }

    @Test
    void createRoleRejectsAPlatformRoleOwnedByCompany() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.createRole("Soporte", null, 7, RoleScope.PLATFORM));
        }
    }

    @Test
    void updateRoleRejectsProtectedSystemRole() {
        Role role = role(20, null, RoleScope.PLATFORM, true);
        role.setProtectedRole(true);
        role.setSystemManaged(true);
        when(roleRepository.findById(20)).thenReturn(Optional.of(role));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.updateRole(20, "Administrador", null, RoleScope.PLATFORM));
        }
    }

    @Test
    void savePermissionsRequiresOptimisticLockVersion() {
        Role role = role(21, company(7), RoleScope.STAFF, true);
        when(roleRepository.findByIdForPermissionUpdate(21)).thenReturn(Optional.of(role));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.saveVistasByRole(21, null, List.of()));
            verify(rolVistaPermisoRepository, never()).deleteByRolId(21);
        }
    }

    @Test
    void deleteRoleRejectsARoleWithAssignedUsers() {
        Role role = role(22, company(7), RoleScope.STAFF, true);
        when(roleRepository.findById(22)).thenReturn(Optional.of(role));
        when(usuarioPorRolRepository.existsByRolId(22)).thenReturn(true);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> service.deleteRole(22));
            verify(roleRepository, never()).delete(role);
        }
    }

    @Test
    void deleteUnusedRoleRemovesItsConfigurationAndAuditsTheAction() {
        Company company = company(7);
        Role role = role(23, company, RoleScope.STAFF, true);
        when(roleRepository.findById(23)).thenReturn(Optional.of(role));
        when(usuarioPorRolRepository.existsByRolId(23)).thenReturn(false);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            service.deleteRole(23);
        }

        verify(rolVentanaPermisoRepository).deleteByRolId(23);
        verify(rolVistaPermisoRepository).deleteByRolId(23);
        verify(roleRepository).delete(role);
        verify(auditLogService).log(7, "ELIMINAR_ROL", "Roles",
                "Se eliminó el rol sin asignaciones ROLE_23");
    }

    private Company company(int id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private Role rolBaseDeAdministrador(int id) {
        Role role = role(id, null, RoleScope.STAFF, true);
        role.setPurpose(RolePurpose.COMPANY_ADMIN);
        role.setSystemManaged(true);
        role.setProtectedRole(true);
        return role;
    }

    private veterinaria.vargasvet.dto.response.RolVistaPermisoDTO permisoDe(int vistaId, boolean leer, boolean modificar) {
        var dto = new veterinaria.vargasvet.dto.response.RolVistaPermisoDTO();
        dto.setVistaId(vistaId);
        dto.setLeer(leer);
        dto.setModificar(modificar);
        return dto;
    }

    private void conVistasEsenciales() {
        for (var par : java.util.Map.of(1, "VISTA_COMPANY", 2, "VISTA_ROLES", 3, "VISTA_EMPLEADOS").entrySet()) {
            Vista vista = vista(par.getKey(), par.getValue(), par.getValue(), par.getKey(), null);
            vista.setAudience(ViewAudience.STAFF);
            when(vistaRepository.findById(par.getKey())).thenReturn(Optional.of(vista));
        }
    }

    @Test
    void platformAdministrationCanChangeThePermissionsOfABaseRoleAndItIsAuditedAsSharedByEveryCompany() {
        Role role = rolBaseDeAdministrador(30);
        when(roleRepository.findByIdForPermissionUpdate(30)).thenReturn(Optional.of(role));
        when(roleRepository.findById(30)).thenReturn(Optional.of(role));
        conVistasEsenciales();

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            service.saveVistasByRole(30, 0L, List.of(permisoDe(1, true, false), permisoDe(2, true, true), permisoDe(3, true, true)));

            verify(rolVistaPermisoRepository).deleteByRolId(30);
            verify(auditLogService).log(eq((Integer) null), eq("MODIFICAR_PERMISOS_ROL_BASE"), eq("Roles"), any());
        }
    }

    @Test
    void theAdministratorRoleCannotLoseAccessToTheViewsThatKeepAClinicGovernable() {
        Role role = rolBaseDeAdministrador(31);
        when(roleRepository.findByIdForPermissionUpdate(31)).thenReturn(Optional.of(role));
        conVistasEsenciales();

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var error = assertThrows(IllegalArgumentException.class,
                    () -> service.saveVistasByRole(31, 0L, List.of(permisoDe(1, true, false), permisoDe(3, true, true))));

            assertTrue(error.getMessage().contains("VISTA_ROLES"));
            verify(rolVistaPermisoRepository, never()).deleteByRolId(31);
        }
    }

    @Test
    void thePlatformAdministrationRoleItselfStaysImmutable() {
        Role role = rolBaseDeAdministrador(32);
        role.setPurpose(RolePurpose.PLATFORM_ADMIN);
        role.setScope(RoleScope.PLATFORM);
        when(roleRepository.findByIdForPermissionUpdate(32)).thenReturn(Optional.of(role));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.saveVistasByRole(32, 0L, List.of()));
            verify(rolVistaPermisoRepository, never()).deleteByRolId(32);
        }
    }

    @Test
    void aCompanyAdministratorCannotChangeABaseRole() {
        Role role = rolBaseDeAdministrador(33);
        when(roleRepository.findByIdForPermissionUpdate(33)).thenReturn(Optional.of(role));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            assertThrows(AccessDeniedException.class, () -> service.saveVistasByRole(33, 0L, List.of()));
            verify(rolVistaPermisoRepository, never()).deleteByRolId(33);
        }
    }

    private Role role(int id, Company company, RoleScope scope, boolean active) {
        Role role = new Role();
        role.setId(id);
        role.setName("ROLE_" + id);
        role.setCompany(company);
        role.setScope(scope);
        role.setActivo(active);
        return role;
    }

    private Vista vista(int id, String code, String name, int order, Ventana window) {
        Vista vista = new Vista();
        vista.setId(id);
        vista.setCodigo(code);
        vista.setNombre(name);
        vista.setRuta("/" + code.toLowerCase());
        vista.setOrden(order);
        vista.setAudience(ViewAudience.SHARED);
        vista.setActivo(true);
        vista.setVentana(window);
        return vista;
    }

    private RolVistaPermiso readablePermission(Role role, Vista vista) {
        RolVistaPermiso permission = new RolVistaPermiso();
        permission.setRol(role);
        permission.setVista(vista);
        permission.setLeer(true);
        return permission;
    }
}
