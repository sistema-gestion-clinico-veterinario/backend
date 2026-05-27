package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "El token es obligatorio")
    @Size(max = 255, message = "El token no debe superar 255 caracteres")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La nueva contrasena debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La nueva contrasena no debe contener espacios")
    private String newPassword;
}
