package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.CanalSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudCancelacionResponse(
        Long id,
        EstadoSolicitudCancelacion estado,
        CanalSolicitudCancelacion canal,
        boolean identidadVerificada,
        String metodoVerificacion,
        String motivoTitular,
        LocalDateTime creadaAt,
        LocalDate fechaLimite,
        boolean vencida,
        LocalDateTime resueltaAt,
        String motivoResolucion,
        String titularNombre,
        String titularCorreo,
        String titularDocumento,
        boolean puedeEjecutarse,
        String impedimento) {
}
