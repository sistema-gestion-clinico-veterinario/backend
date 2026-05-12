package veterinaria.vargasvet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangePasswordRequest {
    @NotNull(message = "El ID de usuario es obligatorio")
    private Integer userId;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String newPassword;
}
