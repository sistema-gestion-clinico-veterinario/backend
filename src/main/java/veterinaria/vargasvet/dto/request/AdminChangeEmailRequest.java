package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminChangeEmailRequest {
    @NotBlank(message = "El nuevo correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String newEmail;
}
