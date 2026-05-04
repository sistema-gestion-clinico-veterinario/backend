package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsultaResumenResponse {
    private Long id;
    private LocalDateTime fechaConsulta;
    private String motivoConsulta;
    private String tipoConsulta;
    private String veterinarioNombre;
}
