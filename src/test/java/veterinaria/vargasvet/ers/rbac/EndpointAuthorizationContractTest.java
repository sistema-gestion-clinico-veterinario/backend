package veterinaria.vargasvet.ers.rbac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.controller.ApoderadoController;
import veterinaria.vargasvet.controller.ConsultaController;
import veterinaria.vargasvet.controller.MascotaController;
import veterinaria.vargasvet.controller.PrescripcionController;
import veterinaria.vargasvet.controller.RoleController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointAuthorizationContractTest {

    @Test
    @DisplayName("[CP-RNF02-01] Los endpoints críticos declaran la acción RBAC correspondiente")
    void endpointsCriticosTienenAutorizacionPorAccion() {
        List<ExpectedAuthorization> expected = List.of(
                expected(ApoderadoController.class, "listar", "VISTA_CLIENTES", "LEER"),
                expected(ApoderadoController.class, "findById", "VISTA_CLIENTES", "LEER"),
                expected(ApoderadoController.class, "registerApoderado", "VISTA_CLIENTES", "ESCRIBIR"),
                expected(ApoderadoController.class, "updateApoderado", "VISTA_CLIENTES", "MODIFICAR"),
                expected(ApoderadoController.class, "eliminar", "VISTA_CLIENTES", "ELIMINAR"),
                expected(ApoderadoController.class, "cambiarEstado", "VISTA_CLIENTES", "MODIFICAR"),
                expected(MascotaController.class, "listar", "VISTA_MASCOTAS", "LEER"),
                expected(MascotaController.class, "obtenerPorId", "VISTA_MASCOTAS", "LEER"),
                expected(MascotaController.class, "registerMascota", "VISTA_MASCOTAS", "ESCRIBIR"),
                expected(MascotaController.class, "updateMascota", "VISTA_MASCOTAS", "MODIFICAR"),
                expected(MascotaController.class, "cambiarEstado", "VISTA_MASCOTAS", "MODIFICAR"),
                expected(ConsultaController.class, "getConsultaById", "VISTA_HISTORIAS", "LEER"),
                expected(ConsultaController.class, "updateConsulta", "VISTA_HISTORIAS", "MODIFICAR"),
                expected(ConsultaController.class, "cerrarConsulta", "VISTA_HISTORIAS", "MODIFICAR"),
                expected(PrescripcionController.class, "buscar", "VISTA_HISTORIAS", "LEER"),
                expected(PrescripcionController.class, "crear", "VISTA_HISTORIAS", "ESCRIBIR"),
                expected(PrescripcionController.class, "listarPorConsulta", "VISTA_HISTORIAS", "LEER"),
                expected(PrescripcionController.class, "actualizar", "VISTA_HISTORIAS", "MODIFICAR"),
                expected(PrescripcionController.class, "eliminar", "VISTA_HISTORIAS", "ELIMINAR"),
                expected(RoleController.class, "getAllRoles", "VISTA_ROLES", "LEER"),
                expected(RoleController.class, "createRole", "VISTA_ROLES", "ESCRIBIR"),
                expected(RoleController.class, "updateRole", "VISTA_ROLES", "MODIFICAR"),
                expected(RoleController.class, "deleteRole", "VISTA_ROLES", "ELIMINAR"),
                expected(RoleController.class, "saveVistasVersioned", "VISTA_ROLES", "MODIFICAR")
        );

        expected.forEach(item -> {
            Method method = Arrays.stream(item.controller().getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(item.method()))
                    .findFirst()
                    .orElseThrow();
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("%s.%s debe declarar @PreAuthorize", item.controller().getSimpleName(), item.method())
                    .isNotNull();
            assertThat(annotation.value())
                    .contains("'" + item.view() + "'", "'" + item.action() + "'");
        });
    }

    private ExpectedAuthorization expected(
            Class<?> controller, String method, String view, String action) {
        return new ExpectedAuthorization(controller, method, view, action);
    }

    private record ExpectedAuthorization(
            Class<?> controller, String method, String view, String action) {
    }
}
