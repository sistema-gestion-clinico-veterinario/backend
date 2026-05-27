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

@Data
public class ApoderadoRequest {
    private Long id;
    @NotBlank
    @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
    private String nombre;
    @NotBlank
    @Size(min = 2, max = 80, message = "El apellido debe tener entre 2 y 80 caracteres")
    private String apellido;
    @NotNull
    private TipoDocumentoIdentidad tipoDocumento;
    @NotBlank
    @Size(max = 20, message = "El documento no debe superar 20 caracteres")
    private String numeroDocumento;
    @NotBlank
    @Email(message = "Formato de correo electrónico inválido")
    private String email;
    @NotBlank
    @Pattern(regexp = "^[0-9]{9}$", message = "El telefono debe contener 9 digitos numericos")
    private String telefono;
    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 200, message = "La direccion no debe superar 200 caracteres")
    private String direccion;
    @NotNull
    private Genero genero;
    private String referencias;
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
