package veterinaria.vargasvet.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.shared.DiaSemana;

import java.time.LocalTime;

@Data
public class HorarioEmpleadoRequest {

    @NotNull(message = "El día de la semana es obligatorio")
    private DiaSemana diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    private Boolean activo = true;
}
