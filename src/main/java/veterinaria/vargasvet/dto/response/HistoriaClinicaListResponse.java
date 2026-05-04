package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoriaClinicaListResponse {
    private Long id;
    private String numeroHc;
    private String mascotaNombre;
    private String propietarioNombre;
    private LocalDateTime fechaUltimaConsulta;
    private Boolean activa;
}
