package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleReportResponse {
    private Long empleadoId;
    private String nombreCompleto;
    private String cargo;
    private List<HorarioEmpleadoResponse> horarios;
}
