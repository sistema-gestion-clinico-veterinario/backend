package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CitaRequest {

    @NotNull(message = "El ID de la mascota es obligatorio")
    private Long mascotaId;

    @NotNull(message = "El ID del veterinario es obligatorio")
    private Long veterinarioId;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    private String motivoCita;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHoraInicio;

    private Long servicioId;

    private String notas;
    private Long version;
}
