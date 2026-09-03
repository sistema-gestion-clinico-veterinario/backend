package veterinaria.vargasvet.service;

import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.enums.MenuPresentation;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        when(assignmentRepository.findActiveAssignmentByUsuarioIdAndRoleId(10, 7))
                .thenReturn(Optional.of(assignment));
        when(permissionRepository.findByRolIdWithVistaAndVentana(7))
                .thenReturn(List.of(readable(role, client), readable(role, shared), readable(role, staff)));

        var result = service.construirMenuJerarquico(10, 7);

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

        when(assignmentRepository.findActiveAssignmentByUsuarioIdAndRoleId(10, 8))
                .thenReturn(Optional.of(assignment));
        when(permissionRepository.findByRolIdWithVistaAndVentana(8))
                .thenReturn(List.of(readable(role, view)));

        assertEquals(List.of(), service.construirMenuJerarquico(10, 8));
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
