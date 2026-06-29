package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Pattern(regexp = "^\\S+@\\S+\\.\\S+$", message = "El email no debe contener espacios")
    @Size(max = 255, message = "El email no debe superar 255 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contrasena no debe superar 72 caracteres")
    private String password;
}
