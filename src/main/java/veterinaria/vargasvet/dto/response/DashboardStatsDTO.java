package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardStatsDTO {
    private long totalPacientes;
    private long totalCitasHoy;
    private long totalCitas;
    private long totalEmpleados;
    private long totalEmpresas; // Solo para SuperAdmin
    private List<Long> citasPorDia;
    private List<Long> citasPorSemana;
    private List<Long> citasPorMes;
}
