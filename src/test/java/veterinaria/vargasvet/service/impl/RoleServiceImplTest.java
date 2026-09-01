package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.MenuItemType;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.RolVentanaConfiguracionRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.RolVistaConfiguracionRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.security.SecurityUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RoleServiceImplTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RolVistaPermisoRepository rolVistaPermisoRepository = mock(RolVistaPermisoRepository.class);
    private final RolVentanaConfiguracionRepository rolVentanaConfiguracionRepository = mock(RolVentanaConfiguracionRepository.class);
    private final RolVistaConfiguracionRepository rolVistaConfiguracionRepository = mock(RolVistaConfiguracionRepository.class);
    private final RoleServiceImpl service = new RoleServiceImpl(
            roleRepository,
            mock(CompanyRepository.class),
            mock(VistaRepository.class),
            rolVistaPermisoRepository,
            mock(VentanaRepository.class),
            rolVentanaConfiguracionRepository,
            rolVistaConfiguracionRepository
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
    void getMenuOrderCombinesFlatViewsAndModulesUsingConfiguredHierarchy() {
        Role role = role(10, null, RoleScope.PLATFORM, true);
        Vista dashboard = vista(1, "VISTA_DASHBOARD", "Dashboard", 0, null);
        Ventana clinic = new Ventana();
        clinic.setId(5);
        clinic.setCodigo("CLINICA");
        clinic.setNombre("Clínica");
        clinic.setOrden(1);
        clinic.setActivo(true);
        Vista patients = vista(2, "VISTA_PACIENTES", "Pacientes", 0, clinic);

        when(roleRepository.findById(10)).thenReturn(java.util.Optional.of(role));
        when(rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(10))
                .thenReturn(List.of(readablePermission(role, dashboard), readablePermission(role, patients)));
        when(rolVentanaConfiguracionRepository.findByRolIdWithVentana(10)).thenReturn(List.of());
        when(rolVistaConfiguracionRepository.findByRolIdWithVistaAndVentana(10)).thenReturn(List.of());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var result = service.getMenuOrder(10);

            assertEquals(2, result.size());
            assertEquals(MenuItemType.VIEW, result.get(0).getTipo());
            assertEquals(MenuItemType.MODULE, result.get(1).getTipo());
            assertEquals(List.of(2), result.get(1).getVistas().stream()
                    .map(item -> item.getReferenciaId()).toList());
        }
    }

    @Test
    void saveMenuOrderRejectsAnIncompleteStructure() {
        Role role = role(11, null, RoleScope.PLATFORM, true);
        Vista dashboard = vista(1, "VISTA_DASHBOARD", "Dashboard", 0, null);
        when(roleRepository.findById(11)).thenReturn(java.util.Optional.of(role));
        when(rolVistaPermisoRepository.findByRolIdWithVistaAndVentana(11))
                .thenReturn(List.of(readablePermission(role, dashboard)));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> service.saveMenuOrder(11, List.of()));
        }
    }

    private Company company(int id) {
        Company company = new Company();
        company.setId(id);
        return company;
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
