package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangePasswordRequest {
    private Integer userId;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
    private String newPassword;
}
