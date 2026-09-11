package veterinaria.vargasvet.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CerrarConsultaRequest extends ConsultaRequest {
    private RegistroVacunacionRequest registroVacunacion;
    private RegistroDesparasitacionRequest registroDesparasitacion;
}
