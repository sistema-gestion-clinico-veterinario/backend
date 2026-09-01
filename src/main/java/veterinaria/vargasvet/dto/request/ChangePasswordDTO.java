package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 72, message = "La contrasena actual no debe superar 72 caracteres")
    private String oldPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 12, max = 72, message = "La nueva contraseña debe tener al menos 12 caracteres")
    private String newPassword;
}
