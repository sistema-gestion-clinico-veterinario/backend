package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import java.util.List;

@Data
public class ApoderadoPerfilResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String direccion;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private Genero genero;
    private String referencias;
    private String observaciones;
    private Integer companyId;
    private String companyName;
    private String companyPhone;
    private Boolean currentlyOpen;
    private List<MascotaResponse> mascotas;
}
