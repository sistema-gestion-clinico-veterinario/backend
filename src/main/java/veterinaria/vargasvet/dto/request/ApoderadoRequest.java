package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

@Data
public class ApoderadoRequest {
    private Long id;
    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotNull
    private TipoDocumentoIdentidad tipoDocumento;
    @NotBlank
    private String numeroDocumento;
    @NotBlank
    @Email(message = "Formato de correo electrónico inválido")
    private String email;
    @NotBlank
    @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe contener entre 7 y 15 dígitos numéricos")
    private String telefono;
    private String direccion;
    @NotNull
    private Genero genero;
    private String referencias;
    private String observaciones;
    private Integer companyId;
}
