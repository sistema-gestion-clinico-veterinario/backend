package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoCita;

import java.time.LocalDateTime;

@Data
public class CitaResponse {
    private Long id;
    private Long version;
    private Long mascotaId;
    private String mascotaNombre;
    private Long apoderadoId;
    private String apoderadoNombre;
    private Long veterinarioId;
    private String veterinarioNombre;
    private Long servicioId;
    private String servicioNombre;
    private String motivoCita;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private Integer duracionMinutos;
    private EstadoCita estado;
    private String notas;
    private Long consultaId;
}
