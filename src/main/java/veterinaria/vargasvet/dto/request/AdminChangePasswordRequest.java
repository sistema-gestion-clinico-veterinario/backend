package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangePasswordRequest {
    private Integer userId;

    private String email;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres")
    private String newPassword;
}
