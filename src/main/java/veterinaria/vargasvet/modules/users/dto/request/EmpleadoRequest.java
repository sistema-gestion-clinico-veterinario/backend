package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.enums.Genero;
import veterinaria.vargasvet.modules.users.domain.enums.TipoDocumentoIdentidad;

import java.util.List;
import java.util.Set;

@Data
public class EmpleadoRequest {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumentoIdentidad tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numeroDocumento;

    @NotNull(message = "El género es obligatorio")
    private Genero genero;

    private String telefono;
    private String direccion;
    private String fotoUrl;
    private String observaciones;
    private Boolean estado;
    private Integer companyId;

    private String numeroColegiatura;
    private Set<String> especialidades;
    private Set<String> roles;
    private Set<String> tiposEmpleado;
    private List<HorarioEmpleadoRequest> horarios;
}
