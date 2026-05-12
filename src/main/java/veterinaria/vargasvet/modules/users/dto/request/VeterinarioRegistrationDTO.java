package veterinaria.vargasvet.modules.users.dto.request;

import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.enums.Genero;
import veterinaria.vargasvet.modules.users.domain.enums.TipoDocumentoIdentidad;

import java.util.Set;

@Data
public class VeterinarioRegistrationDTO {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private String numeroDocumento;
    private TipoDocumentoIdentidad tipoDocumento;
    private Genero genero;
    private String telefono;
    private String direccion;
    private String numeroColegiatura;
    private String fotoUrl;
    private String observaciones;
    private Set<String> especialidades;
}
