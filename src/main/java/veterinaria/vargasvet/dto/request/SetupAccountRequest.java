package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetupAccountRequest {
    @NotBlank
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contrasena debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La contrasena no debe contener espacios")
    private String password;
}
