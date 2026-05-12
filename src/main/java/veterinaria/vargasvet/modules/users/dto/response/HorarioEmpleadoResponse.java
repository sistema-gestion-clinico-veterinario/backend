package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;
import java.time.LocalTime;

@Data
public class HorarioEmpleadoResponse {
    private Long id;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
}
