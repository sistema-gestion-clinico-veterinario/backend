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
    @Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El email debe estar en minusculas y tener un formato valido")
    @Size(max = 255, message = "El email no debe superar 255 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contrasena no debe superar 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La contraseña no debe contener espacios")
    private String password;
}
