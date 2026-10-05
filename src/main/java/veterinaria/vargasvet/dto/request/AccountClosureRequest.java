package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountClosureRequest {
    @Size(max = 128, message = "La contraseña no es válida")
    private String password;
}
