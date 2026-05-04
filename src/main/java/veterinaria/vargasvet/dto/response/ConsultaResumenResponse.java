package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsultaResumenResponse {
    private Long id;
    private LocalDateTime fechaConsulta;
    private String motivoConsulta;
    private String tipoConsulta;
    private String veterinarioNombre;
    private List<ArchivoClinicoResponse> archivos;
}
