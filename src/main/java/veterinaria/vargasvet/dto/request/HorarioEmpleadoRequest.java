package veterinaria.vargasvet.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.DiaSemana;

import java.time.LocalTime;

@Data
public class HorarioEmpleadoRequest {

    private java.time.LocalDate fecha;

    private DiaSemana diaSemana;

    @NotNull(message = "La hora de inicio es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaFin;

    private Boolean activo = true;
}
