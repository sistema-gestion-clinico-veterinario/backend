package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestEmailChangeDTO {

    @NotBlank(message = "La contraseña actual es requerida")
    @Size(max = 72, message = "La contraseña actual no es válida")
    private String currentPassword;

    @NotBlank(message = "El nuevo correo es requerido")
    @Email(message = "El nuevo correo no es válido")
    @Size(max = 254, message = "El nuevo correo es demasiado largo")
    private String newEmail;
}
