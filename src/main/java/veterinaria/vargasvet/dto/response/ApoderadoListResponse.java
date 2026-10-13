package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
import veterinaria.vargasvet.dto.ProtectedPersonalData;
import veterinaria.vargasvet.util.DataMasking;

@Data
public class ApoderadoListResponse implements ProtectedPersonalData {
    private Long id;
    private Integer userId;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private TipoDocumentoIdentidad tipoDocumento;
    private String numeroDocumento;
    private Boolean activo;
    private Boolean cuentaPendiente;
    private Boolean puedeReenviarInvitacion;
    private Boolean avisoInformado;
    private veterinaria.vargasvet.domain.enums.TipoInactividad tipoInactividad;

    @Override
    public String authorizingView() {
        return "VISTA_CLIENTES";
    }

    @Override
    public void maskSensitiveData() {
        email = DataMasking.correo(email);
        telefono = DataMasking.telefono(telefono);
        numeroDocumento = DataMasking.documento(numeroDocumento);
    }
}
