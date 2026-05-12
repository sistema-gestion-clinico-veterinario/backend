package veterinaria.vargasvet.modules.users.dto.request;

import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
public class HorarioEmpleadoRequest {
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Boolean activo;
}
