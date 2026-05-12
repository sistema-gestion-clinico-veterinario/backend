package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;
import veterinaria.vargasvet.modules.users.domain.enums.TipoDocumentoIdentidad;

@Data
public class ApoderadoListResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private boolean activo;
}
