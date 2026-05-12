package veterinaria.vargasvet.citas;

import lombok.Data;
import veterinaria.vargasvet.shared.EstadoCita;

import java.math.BigDecimal;
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
    private Boolean esEmergencia;
    private BigDecimal totalServicio;
    private BigDecimal montoPagado;
}
