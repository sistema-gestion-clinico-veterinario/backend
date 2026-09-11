package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CerrarConsultaRequest extends ConsultaRequest {
    @Valid
    private RegistroVacunacionRequest registroVacunacion;
    @Valid
    private RegistroDesparasitacionRequest registroDesparasitacion;
}
