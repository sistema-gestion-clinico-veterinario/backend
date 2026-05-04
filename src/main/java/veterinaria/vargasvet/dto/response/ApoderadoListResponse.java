package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;

@Data
public class ApoderadoListResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private Boolean activo;
}
