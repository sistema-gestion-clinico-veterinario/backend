package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoCita;

import java.time.LocalDateTime;

/** Datos operativos mínimos usados para las métricas del dashboard. */
@Data
@Builder
public class DashboardAppointmentSummary {
    private Long id;
    private Long veterinarioId;
    private String servicioNombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
}
