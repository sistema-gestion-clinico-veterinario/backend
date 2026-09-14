package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {
    /** Slug de la empresa resuelto por la URL (systemvet.com/<slug>/login) -
     * decide contra cual empresa se valida la membresia, antes de cualquier
     * pantalla de seleccion. Opcional: ausente en el login "global" (la
     * pantalla sin marca de ninguna empresa en particular), donde solo se
     * permite si el username tiene exactamente una empresa activa. */
    @Size(max = 100, message = "El slug no debe superar 100 caracteres")
    private String slug;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no debe superar 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contrasena no debe superar 72 caracteres")
    private String password;
}
