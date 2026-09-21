package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class CitaReasignacionVeterinarioRequest {

    @NotNull(message = "El nuevo veterinario es obligatorio")
    private Long veterinarioId;

    @Size(max = 250, message = "El motivo no debe superar 250 caracteres")
    @MeaningfulText(message = "El motivo debe contener texto real, no solo numeros o simbolos")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "El motivo contiene caracteres no permitidos")
    private String motivo;
}
