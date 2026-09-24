package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.validation.StrongPassword;

@Data
public class SetupAccountRequest {
    @NotBlank
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 72, message = "La contraseña debe tener al menos 12 caracteres")
    @StrongPassword
    private String password;
}
