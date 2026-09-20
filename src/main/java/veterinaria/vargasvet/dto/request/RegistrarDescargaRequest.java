package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDescarga;

@Data
public class RegistrarDescargaRequest {

    @NotNull(message = "El tipo de descarga es obligatorio")
    private TipoDescarga tipo;

    @Size(max = 150, message = "La referencia no debe superar 150 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "La referencia contiene caracteres no permitidos")
    private String referencia;
}
