package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalPacientes;
    private long totalCitasHoy;
    private long totalCitas;
    private long totalEmpleados;
    private long totalEmpresas; // Solo para SuperAdmin
}
