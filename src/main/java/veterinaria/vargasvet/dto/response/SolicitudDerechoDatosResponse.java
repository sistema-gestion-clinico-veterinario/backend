package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;
import veterinaria.vargasvet.domain.enums.TipoDerechoDatos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudDerechoDatosResponse(
        Long id, TipoDerechoDatos tipo, EstadoSolicitudDerecho estado, String descripcion,
        String datoRectificar, boolean identidadVerificada, String metodoVerificacion,
        LocalDateTime creadaAt, LocalDate fechaLimite, boolean vencida,
        String respuesta, String motivoRechazo, LocalDateTime resueltaAt,
        String titular, String correoTitular) {
}
