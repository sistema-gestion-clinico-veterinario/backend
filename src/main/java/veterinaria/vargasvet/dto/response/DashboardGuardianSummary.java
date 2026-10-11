package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Datos mínimos de un propietario o autorizado necesarios para el dashboard.
 */
@Data
@Builder
public class DashboardGuardianSummary {
    private Long id;
    private String nombre;
    private String apellido;
    private Boolean activo;
}
