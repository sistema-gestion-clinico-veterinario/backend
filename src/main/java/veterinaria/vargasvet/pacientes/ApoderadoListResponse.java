package veterinaria.vargasvet.pacientes;

import lombok.Data;
import veterinaria.vargasvet.shared.TipoDocumentoIdentidad;

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
