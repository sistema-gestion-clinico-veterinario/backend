package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

/** Información no personal necesaria para el resumen de empresas. */
@Data
@Builder
public class DashboardCompanySummary {
    private Integer id;
    private String name;
    private Boolean activo;
}
