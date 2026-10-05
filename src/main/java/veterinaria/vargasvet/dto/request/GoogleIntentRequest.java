package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleIntentRequest {
    @Size(max = 100, message = "El identificador de la clínica no es válido")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "El identificador de la clínica no es válido")
    private String slug;

    @Size(max = 200, message = "El enlace de activación no es válido")
    private String activationToken;
}
