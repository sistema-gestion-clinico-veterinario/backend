package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoriaClinicaListResponse {
    private Long id;
    private String numeroHc;
    private Long mascotaId;
    private String mascotaNombre;
    private String especie;
    private String raza;
    private String propietarioNombre;
    private Integer companyId;
    private String companyName;
    private LocalDateTime fechaUltimaConsulta;
    private Boolean activa;
}
