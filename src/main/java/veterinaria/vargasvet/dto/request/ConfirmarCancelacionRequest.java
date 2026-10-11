package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmarCancelacionRequest {
    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código debe tener 6 dígitos")
    private String code;

    @Size(max = 500, message = "El motivo no debe superar 500 caracteres")
    private String motivo;
}
