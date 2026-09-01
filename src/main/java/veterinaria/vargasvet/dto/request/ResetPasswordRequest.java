package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "El token es obligatorio")
    @Size(max = 255, message = "El token no debe superar 255 caracteres")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 12, max = 72, message = "La nueva contraseña debe tener al menos 12 caracteres")
    private String newPassword;
}
