package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

@Data
public class PublicarAvisoPrivacidadRequest {

    @NotNull(message = "Selecciona a quién está dirigido el aviso")
    private AudienciaAvisoPrivacidad audiencia = AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS;

    @Valid
    @NotNull(message = "Los datos del aviso son obligatorios")
    private CamposAvisoPrivacidad campos;

    @AssertTrue(message = "Confirma que el texto fue revisado por tu asesor legal antes de publicarlo")
    private boolean confirmoRevisionLegal;
}
