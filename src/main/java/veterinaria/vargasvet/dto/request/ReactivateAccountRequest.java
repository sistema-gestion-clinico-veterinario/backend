package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReactivateAccountRequest {
    @NotBlank(message = "El enlace no es válido")
    @Size(max = 200, message = "El enlace no es válido")
    private String token;
}
