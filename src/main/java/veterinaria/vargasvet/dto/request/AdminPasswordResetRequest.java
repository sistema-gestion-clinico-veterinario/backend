package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class AdminPasswordResetRequest {
    private Integer userId;

    @Email(message = "El correo electrónico no es válido")
    private String email;
}
