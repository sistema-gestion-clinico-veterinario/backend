package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.DataScope;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MenuBuilderServiceTest {

    private final UsuarioPorRolRepository assignmentRepository = mock(UsuarioPorRolRepository.class);
    private final RolVistaPermisoRepository permissionRepository = mock(RolVistaPermisoRepository.class);
    private final MenuBuilderService service = new MenuBuilderService(assignmentRepository, permissionRepository);

    @Test
    void clientPortalOnlyReceivesClientAndSharedNavigation() {
        Role role = new Role();
        role.setId(7);
        role.setScope(RoleScope.CLIENT);
        role.setPurpose(RolePurpose.CLIENT_PORTAL);

        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);

        Vista client = view(1, "VISTA_MIS_MASCOTAS", ViewAudience.CLIENT, null);
        Vista shared = view(2, "VISTA_PROFILE", ViewAudience.SHARED, null);
        Vista staff = view(3, "VISTA_EMPLEADOS", ViewAudience.STAFF, null);

        when(assignmentRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(10, 7, 3))
                .thenReturn(Optional.of(assignment));
        when(permissionRepository.findByRolIdWithVistaAndVentana(7))
                .thenReturn(List.of(readable(role, client), readable(role, shared), readable(role, staff)));

        var result = service.construirMenuJerarquico(10, 7, 3);

        assertEquals(List.of("VISTA_MIS_MASCOTAS", "VISTA_PROFILE"), result.stream()
                .flatMap(section -> section.getVistas().stream())
                .map(item -> item.getCodigo())
                .toList());
    }

    @Test
    void viewFromInactiveModuleIsNotPromotedToFlatNavigation() {
        Role role = new Role();
        role.setId(8);
        role.setScope(RoleScope.STAFF);

        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);

        Ventana inactiveModule = new Ventana();
        inactiveModule.setId(20);
        inactiveModule.setActivo(false);
        inactiveModule.setPresentacionDefault(MenuPresentation.GROUPED);
        Vista view = view(4, "VISTA_CLIENTES", ViewAudience.STAFF, inactiveModule);

        when(assignmentRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(10, 8, 3))
                .thenReturn(Optional.of(assignment));
        when(permissionRepository.findByRolIdWithVistaAndVentana(8))
                .thenReturn(List.of(readable(role, view)));

        assertEquals(List.of(), service.construirMenuJerarquico(10, 8, 3));
    }

    private Role staffRole(int id, RolePurpose purpose) {
        Role role = new Role();
        role.setId(id);
        role.setScope(RoleScope.STAFF);
        role.setPurpose(purpose);
        return role;
    }

    private void assign(Role role, Integer userId, Integer companyId) {
        UsuarioPorRol assignment = new UsuarioPorRol();
        assignment.setRol(role);
        when(assignmentRepository.findActiveAssignmentByUsuarioIdAndRoleIdAndCompanyId(userId, role.getId(), companyId))
                .thenReturn(Optional.of(assignment));
    }

    private RolVistaPermiso permission(Role role, Vista view, boolean read, boolean write) {
        RolVistaPermiso permission = readable(role, view);
        permission.setLeer(read);
        permission.setEscribir(write);
        return permission;
    }

    private List<String> codes(List<veterinaria.vargasvet.dto.response.MenuStructureDTO> menu) {
        return menu.stream().flatMap(section -> section.getVistas().stream()).map(item -> item.getCodigo()).toList();
    }

    @Test
    void aViewWithoutReadPermissionIsHiddenEvenWhenTheRoleCanWriteIt() {
        Role role = staffRole(11, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        Vista readable = view(1, "VISTA_CLIENTES", ViewAudience.STAFF, null);
        Vista writeOnly = view(2, "VISTA_EMPLEADOS", ViewAudience.STAFF, null);
        when(permissionRepository.findByRolIdWithVistaAndVentana(11))
                .thenReturn(List.of(permission(role, readable, true, false), permission(role, writeOnly, false, true)));

        assertEquals(List.of("VISTA_CLIENTES"), codes(service.construirMenuJerarquico(10, 11, 3)));
        assertEquals(List.of("VISTA_CLIENTES"), service.construirMenu(10, 11, 3).stream()
                .map(item -> item.getCodigo()).toList());
    }

    @Test
    void inactiveViewsAndViewsHiddenFromTheMenuAreNotListed() {
        Role role = staffRole(12, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        Vista visible = view(1, "VISTA_CLIENTES", ViewAudience.STAFF, null);
        Vista inactive = view(2, "VISTA_EMPLEADOS", ViewAudience.STAFF, null);
        inactive.setActivo(false);
        Vista support = view(3, "VISTA_PRIVACIDAD", ViewAudience.STAFF, null);
        support.setVisibleMenu(false);
        when(permissionRepository.findByRolIdWithVistaAndVentana(12))
                .thenReturn(List.of(readable(role, visible), readable(role, inactive), readable(role, support)));

        assertEquals(List.of("VISTA_CLIENTES"), codes(service.construirMenuJerarquico(10, 12, 3)));
    }

    @Test
    void aSessionWithoutAnActiveRoleReceivesNoNavigation() {
        assertEquals(List.of(), service.construirMenuJerarquico(10, null, 3));
        assertEquals(List.of(), service.construirMenu(10, null, 3));
        assertEquals(List.of(), service.construirPermissions(10, null, 3));
    }

    @Test
    void aRoleThatIsNoLongerAssignedInThatCompanyReceivesNoNavigation() {
        Role role = staffRole(13, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        when(permissionRepository.findByRolIdWithVistaAndVentana(13))
                .thenReturn(List.of(readable(role, view(1, "VISTA_CLIENTES", ViewAudience.STAFF, null))));

        assertEquals(List.of("VISTA_CLIENTES"), codes(service.construirMenuJerarquico(10, 13, 3)));
        assertEquals(List.of(), service.construirMenuJerarquico(10, 13, 4));
        assertEquals(List.of(), service.construirMenuJerarquico(99, 13, 3));
    }

    @Test
    void aRoleWithoutAnyPermissionReceivesAnEmptyMenu() {
        Role role = staffRole(14, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        when(permissionRepository.findByRolIdWithVistaAndVentana(14)).thenReturn(List.of());

        assertEquals(List.of(), service.construirMenuJerarquico(10, 14, 3));
    }

    @Test
    void theNavigationOfOneRoleNeverIncludesTheViewsOfAnother() {
        Role vet = staffRole(15, RolePurpose.CUSTOM);
        Role reception = staffRole(16, RolePurpose.CUSTOM);
        assign(vet, 10, 3);
        assign(reception, 10, 3);
        Vista historias = view(1, "VISTA_HISTORIAS", ViewAudience.STAFF, null);
        Vista caja = view(2, "VISTA_CAJA", ViewAudience.STAFF, null);
        when(permissionRepository.findByRolIdWithVistaAndVentana(15)).thenReturn(List.of(readable(vet, historias)));
        when(permissionRepository.findByRolIdWithVistaAndVentana(16)).thenReturn(List.of(readable(reception, caja)));

        assertEquals(List.of("VISTA_HISTORIAS"), codes(service.construirMenuJerarquico(10, 15, 3)));
        assertEquals(List.of("VISTA_CAJA"), codes(service.construirMenuJerarquico(10, 16, 3)));
    }

    @Test
    void platformAdministrationSeesEveryAudienceAndCompanyWideScope() {
        Role platform = new Role();
        platform.setId(17);
        platform.setScope(RoleScope.PLATFORM);
        platform.setPurpose(RolePurpose.PLATFORM_ADMIN);
        assign(platform, 10, null);
        when(permissionRepository.findByRolIdWithVistaAndVentana(17)).thenReturn(List.of(
                readable(platform, view(1, "VISTA_COMPANY", ViewAudience.STAFF, null)),
                readable(platform, view(2, "VISTA_MIS_MASCOTAS", ViewAudience.CLIENT, null)),
                readable(platform, view(3, "VISTA_PROFILE", ViewAudience.SHARED, null))));

        var menu = service.construirMenu(10, 17, null);

        assertEquals(3, menu.size());
        assertTrue(menu.stream().allMatch(item -> item.getDataScope() == DataScope.COMPANY));
    }

    @Test
    void aCustomRoleKeepsItsOwnDataScopeInTheMenu() {
        Role role = staffRole(18, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        RolVistaPermiso agenda = readable(role, view(1, "VISTA_CITAS_AGENDA", ViewAudience.STAFF, null));
        agenda.setDataScope(DataScope.OWN);
        RolVistaPermiso historias = readable(role, view(2, "VISTA_HISTORIAS", ViewAudience.STAFF, null));
        historias.setDataScope(DataScope.COMPANY);
        when(permissionRepository.findByRolIdWithVistaAndVentana(18)).thenReturn(List.of(agenda, historias));

        var menu = service.construirMenu(10, 18, 3);

        assertEquals(DataScope.OWN, menu.get(0).getDataScope());
        assertEquals(DataScope.COMPANY, menu.get(1).getDataScope());
    }

    @Test
    void activeModulesGroupTheirViewsAndFollowTheConfiguredOrder() {
        Role role = staffRole(19, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        Ventana module = new Ventana();
        module.setId(30);
        module.setCodigo("ADMIN");
        module.setNombre("Administración");
        module.setActivo(true);
        module.setOrden(2);
        module.setPresentacionDefault(MenuPresentation.GROUPED);
        Vista second = view(5, "VISTA_EMPLEADOS", ViewAudience.STAFF, module);
        Vista first = view(1, "VISTA_CLIENTES", ViewAudience.STAFF, module);
        when(permissionRepository.findByRolIdWithVistaAndVentana(19))
                .thenReturn(List.of(readable(role, second), readable(role, first)));

        var menu = service.construirMenuJerarquico(10, 19, 3);

        assertEquals(1, menu.size());
        assertEquals(MenuPresentation.GROUPED, menu.get(0).getPresentacion());
        assertEquals(List.of("VISTA_CLIENTES", "VISTA_EMPLEADOS"), codes(menu));
    }

    @Test
    void thePermissionListOnlyContainsTheActionsThatWereGranted() {
        Role role = staffRole(20, RolePurpose.CUSTOM);
        assign(role, 10, 3);
        when(permissionRepository.findByRolIdWithVistaAndVentana(20)).thenReturn(List.of(
                permission(role, view(1, "VISTA_CLIENTES", ViewAudience.STAFF, null), true, false),
                permission(role, view(2, "VISTA_EMPLEADOS", ViewAudience.STAFF, null), true, true)));

        var permissions = service.construirPermissions(10, 20, 3);

        assertTrue(permissions.contains("VISTA_CLIENTES:LEER"));
        assertTrue(!permissions.contains("VISTA_CLIENTES:ESCRIBIR"));
        assertTrue(permissions.contains("VISTA_EMPLEADOS:ESCRIBIR"));
        assertTrue(!permissions.contains("VISTA_EMPLEADOS:ELIMINAR"));
    }

    private Vista view(int id, String code, ViewAudience audience, Ventana module) {
        Vista view = new Vista();
        view.setId(id);
        view.setCodigo(code);
        view.setNombre(code);
        view.setRuta("/legacy");
        view.setOrden(id);
        view.setActivo(true);
        view.setVisibleMenu(true);
        view.setAudience(audience);
        view.setVentana(module);
        return view;
    }

    private RolVistaPermiso readable(Role role, Vista view) {
        RolVistaPermiso permission = new RolVistaPermiso();
        permission.setRol(role);
        permission.setVista(view);
        permission.setLeer(true);
        return permission;
    }
}
