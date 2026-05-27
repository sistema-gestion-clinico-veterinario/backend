package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationDTO {
    @Email(message = "El correo electrónico no es válido")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Size(max = 255, message = "El correo no debe superar 255 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contrasena debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^\\S+$", message = "La contrasena no debe contener espacios")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    private String apellido;

    @Pattern(regexp = "^$|^\\d{8}$", message = "El DNI debe tener 8 digitos")
    private String dni;

    @Pattern(regexp = "^$|^\\d{9}$", message = "El telefono debe tener 9 digitos")
    private String telefono;

    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    private String direccion;
    private Integer companyId;
}
