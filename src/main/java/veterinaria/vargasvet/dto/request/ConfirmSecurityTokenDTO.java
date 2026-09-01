package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmSecurityTokenDTO {
    @NotBlank(message = "El token es requerido")
    @Size(max = 256, message = "El token no es válido")
    private String token;
}
