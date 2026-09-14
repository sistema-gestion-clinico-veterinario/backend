package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Login del SuperAdmin (PLATFORM_ADMIN) - ruta reservada aparte del login por
 * slug de empresa, ya que un SuperAdmin no pertenece a una empresa en
 * particular (supervisa todas). Ver AuthController /auth/admin-login.
 */
@Data
public class AdminLoginDTO {
    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no debe superar 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contrasena no debe superar 72 caracteres")
    private String password;
}
