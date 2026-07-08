package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangePasswordRequest {
    private Integer userId;

    @Size(max = 100, message = "El correo no debe superar 100 caracteres")
    @Pattern(regexp = "^$|^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El correo debe estar en minusculas y tener un formato valido")
    private String email;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La nueva contrasena debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La nueva contrasena no debe contener espacios")
    private String newPassword;
}
