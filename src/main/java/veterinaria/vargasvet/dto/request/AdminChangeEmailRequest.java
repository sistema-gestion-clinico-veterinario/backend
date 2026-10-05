package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminChangeEmailRequest {
    @NotBlank(message = "El nuevo correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 254, message = "El nuevo correo es demasiado largo")
    private String newEmail;

    /** Nota opcional para la auditoría (por ejemplo, cómo se verificó la identidad). */
    @Size(max = 300, message = "La nota no debe superar 300 caracteres")
    private String motivo;
}
