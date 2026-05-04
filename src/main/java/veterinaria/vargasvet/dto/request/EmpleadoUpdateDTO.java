package veterinaria.vargasvet.dto.request;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class EmpleadoUpdateDTO {
    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private Genero genero;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    

    private Set<String> roles;
    private Set<String> tiposEmpleado;
    private Set<String> especialidades;
    private String fotoUrl;
    private String observaciones;
    private Boolean estado;
}
