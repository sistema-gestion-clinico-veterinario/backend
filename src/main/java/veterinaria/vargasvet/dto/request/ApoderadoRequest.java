package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.validation.MeaningfulText;

@Data
public class ApoderadoRequest {
    private Long id;
    @NotBlank
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;
    @NotBlank
    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "El apellido solo debe contener letras y espacios")
    private String apellido;
    @NotNull
    private TipoDocumentoIdentidad tipoDocumento;
    @NotBlank
    @Size(max = 20, message = "El documento no debe superar 20 caracteres")
    private String numeroDocumento;
    @NotBlank
    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 100, message = "El correo no debe superar 100 caracteres")
    @Pattern(regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El correo debe estar en minusculas y tener un formato valido")
    private String email;
    @NotBlank
    @Pattern(regexp = "^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String telefono;
    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.,#\\-/°:]+$", message = "La direccion contiene caracteres no permitidos")
    @MeaningfulText(message = "La direccion debe contener texto real, no solo numeros o simbolos")
    private String direccion;
    @NotNull
    private Genero genero;
    @Size(max = 500, message = "Las referencias no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las referencias contienen caracteres no permitidos")
    @MeaningfulText(message = "Las referencias deben contener texto real, no solo numeros o simbolos")
    private String referencias;
    @Size(max = 500, message = "Las observaciones no deben superar 500 caracteres")
    @Pattern(regexp = "^$|(?=.*[\\p{L}\\p{N}])(?=\\S)(?!.*[{}\\[\\]<>*|\\\\^~`=@]).*\\S$", message = "Las observaciones contienen caracteres no permitidos")
    @MeaningfulText(message = "Las observaciones deben contener texto real, no solo numeros o simbolos")
    private String observaciones;
    private Integer companyId;

    @AssertTrue(message = "El numero de documento no corresponde al tipo seleccionado")
    public boolean isNumeroDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) return true;
        return switch (tipoDocumento) {
            case DNI -> numeroDocumento.matches("^\\d{8}$");
            case CARNET_EXTRANJERIA -> numeroDocumento.matches("^\\d{9}$");
            case PASAPORTE -> numeroDocumento.matches("^[A-Za-z]\\d{8}$");
        };
    }
}
