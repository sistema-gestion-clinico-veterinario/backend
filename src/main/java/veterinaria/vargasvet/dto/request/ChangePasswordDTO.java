package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 72, message = "La contrasena actual no debe superar 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La contrasena actual no debe contener espacios")
    private String oldPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La nueva contrasena debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La nueva contrasena no debe contener espacios")
    private String newPassword;
}
