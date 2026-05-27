package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CitaRequest {

    @NotNull(message = "El ID de la mascota es obligatorio")
    private Long mascotaId;

    @NotNull(message = "El ID del veterinario es obligatorio")
    private Long veterinarioId;

    @NotBlank(message = "El motivo de la cita es obligatorio")
    @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres")
    private String motivoCita;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "El servicio es obligatorio")
    private Long servicioId;

    @Size(max = 500, message = "Las notas no deben superar 500 caracteres")
    private String notas;
    private Long version;
    private Boolean esEmergencia = false;
}
