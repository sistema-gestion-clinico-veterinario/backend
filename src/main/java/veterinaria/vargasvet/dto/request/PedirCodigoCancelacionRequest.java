package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PedirCodigoCancelacionRequest {
    @Size(max = 128, message = "La contraseña no debe superar 128 caracteres")
    @lombok.ToString.Exclude
    private String password;
}
