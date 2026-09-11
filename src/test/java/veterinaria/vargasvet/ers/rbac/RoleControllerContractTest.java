package veterinaria.vargasvet.ers.rbac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import veterinaria.vargasvet.controller.RoleController;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.RoleRequest;
import veterinaria.vargasvet.dto.response.RolDTO;
import veterinaria.vargasvet.dto.response.RolVistaPermisoDTO;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.RoleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleControllerContractTest {

    private final RoleService roleService = mock(RoleService.class);
    private final RoleController controller = new RoleController(roleService);

    @Test
    @DisplayName("[CP-RF06-01] Un administrador de empresa no puede forzar otra empresa al crear un rol")
    void crearRolUsaEmpresaDeLaSesion() {
        RoleRequest request = roleRequest(99);
        when(roleService.createRole("Recepción", "Atención al cliente", 7, RoleScope.STAFF))
                .thenReturn(new RolDTO());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(false);
            security.when(SecurityUtils::getCurrentCompanyId).thenReturn(7);

            var response = controller.createRole(request);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
        }

        verify(roleService).createRole("Recepción", "Atención al cliente", 7, RoleScope.STAFF);
    }

    @Test
    @DisplayName("[CP-RF06-01] El administrador de plataforma puede seleccionar la empresa del rol")
    void superAdminConservaEmpresaSeleccionada() {
        RoleRequest request = roleRequest(99);
        when(roleService.createRole("Recepción", "Atención al cliente", 99, RoleScope.STAFF))
                .thenReturn(new RolDTO());

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);
            controller.createRole(request);
        }

        verify(roleService).createRole("Recepción", "Atención al cliente", 99, RoleScope.STAFF);
    }

    @Test
    @DisplayName("[CP-RF08-01] La actualización versionada de permisos transmite If-Match")
    void permisosVersionadosTransmitenVersionEsperada() {
        List<RolVistaPermisoDTO> permissions = List.of(new RolVistaPermisoDTO());
        when(roleService.saveVistasByRole(12, 4L, permissions)).thenReturn(permissions);

        var response = controller.saveVistasVersioned(12, 4L, permissions);

        assertThat(response.getBody()).isNotNull();
        verify(roleService).saveVistasByRole(12, 4L, permissions);
    }

    @Test
    @DisplayName("[CP-RF06-01] La consulta global exige una empresa seleccionada")
    void listadoEmpresaExigeContexto() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isSuperAdmin).thenReturn(true);

            var response = controller.getRolesByCompany(null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
        }
    }

    private RoleRequest roleRequest(int companyId) {
        RoleRequest request = new RoleRequest();
        request.setName("Recepción");
        request.setDescripcion("Atención al cliente");
        request.setCompanyId(companyId);
        request.setScope(RoleScope.STAFF);
        return request;
    }
}
