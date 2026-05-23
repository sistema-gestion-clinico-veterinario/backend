package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CitaReprogramacionRequest {

    private Long veterinarioId;

    @NotNull(message = "La nueva fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHoraInicio;

    private String motivoReprogramacion;
}
