package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class VeterinarioRegistrationDTO {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido solo debe contener letras y espacios")
    private String apellido;

    @NotBlank(message = "El número de colegiatura es obligatorio")
    @Size(max = 30, message = "El numero de colegiatura no debe superar 30 caracteres")
    @Pattern(regexp = "^(?=.*[\\p{L}\\p{N}])(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*$", message = "El numero de colegiatura contiene caracteres no permitidos")
    private String numeroColegiatura;

    @NotEmpty(message = "Debe especificar al menos una especialidad")
    private Set<String> especialidades;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    @Size(max = 100, message = "El correo no debe superar 100 caracteres")
    @Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El correo debe estar en minusculas y tener un formato valido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String telefono;

    // Campos opcionales
    private String fotoUrl;
    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    @Pattern(regexp = "^$|^[\\p{L}\\p{N}\\s.,#\\-/°:]+$", message = "La direccion contiene caracteres no permitidos")
    private String direccion;
    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las observaciones contienen caracteres no permitidos")
    private String observaciones;

    // Campos adicionales necesarios para la entidad base
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private Genero genero;
}
