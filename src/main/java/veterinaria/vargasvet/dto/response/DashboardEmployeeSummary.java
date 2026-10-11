package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Datos mínimos de un empleado necesarios para el dashboard.
 * Los datos de contacto e identificación pertenecen a la ficha del empleado.
 */
@Data
@Builder
public class DashboardEmployeeSummary {
    private Long id;
    private String nombre;
    private String apellido;
    private Boolean activo;
    private List<String> tiposEmpleado;
    private List<String> especialidades;
}
