package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgendaCountersResponse {
    private long programadas;
    private long enProceso;
    private long completadas;
    private long canceladas;
}
