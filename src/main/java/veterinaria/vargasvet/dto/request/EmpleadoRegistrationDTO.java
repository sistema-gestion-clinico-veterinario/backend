package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class EmpleadoRegistrationDTO {
    
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


    private Integer companyId;
}
