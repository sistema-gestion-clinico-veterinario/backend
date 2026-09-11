package veterinaria.vargasvet.ers.pagos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;
import veterinaria.vargasvet.controller.PagoController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PagoControllerContractTest {

    @Test
    @DisplayName("[CP-RF45-01] El historial expone filtros de cliente, mascota, período y estado")
    void historialExponeTodosLosFiltrosExigidos() {
        Method endpoint = Arrays.stream(PagoController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("listarHistorialPorEmpresa"))
                .findFirst()
                .orElseThrow();

        Set<String> requestParams = Arrays.stream(endpoint.getParameters())
                .map(this::requestParamName)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());

        assertThat(requestParams)
                .as("RF-45 exige filtrar el historial por cliente, mascota, período y estado")
                .contains("clienteId", "mascotaId", "fechaDesde", "fechaHasta", "estado");
    }

    private String requestParamName(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation == null) {
            return "";
        }
        if (!annotation.name().isBlank()) {
            return annotation.name();
        }
        if (!annotation.value().isBlank()) {
            return annotation.value();
        }
        return parameter.getName();
    }
}
