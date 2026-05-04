package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class EmpleadoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo es inválido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{7,15}$", message = "El teléfono debe contener entre 7 y 15 dígitos numéricos")
    private String telefono;

    private String direccion;
    private Genero genero;
    private TipoDocumentoIdentidad tipoDocumento;

    @NotEmpty(message = "Debe asignar al menos un rol")
    private Set<String> roles;

    private Set<String> tiposEmpleado;
    private String numeroColegiatura;
    private Set<String> especialidades;
    private String fotoUrl;
    private String observaciones;
    private Boolean estado;
    private Integer companyId;
}
