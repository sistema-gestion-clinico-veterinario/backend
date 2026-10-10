package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

@Data
public class VistaPreviaAvisoRequest {

    @NotNull(message = "Selecciona a quién está dirigido el aviso")
    private AudienciaAvisoPrivacidad audiencia = AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS;

    @NotNull(message = "Los datos del aviso son obligatorios")
    private CamposAvisoPrivacidad campos;
}
