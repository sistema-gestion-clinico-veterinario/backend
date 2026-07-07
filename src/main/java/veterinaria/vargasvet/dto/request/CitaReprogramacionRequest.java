package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CitaReprogramacionRequest {

    @NotNull(message = "El veterinario es obligatorio")
    private Long veterinarioId;

    @NotNull(message = "La nueva fecha y hora de la cita son obligatorias")
    private LocalDateTime fechaHoraInicio;

    @Size(max = 250, message = "El motivo no debe superar 250 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El motivo contiene caracteres no permitidos")
    private String motivoReprogramacion;
}
