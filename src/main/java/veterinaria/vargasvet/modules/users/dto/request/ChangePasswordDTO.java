package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String oldPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    private String newPassword;
}
