package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.EstadoConsentimientoVeterinario;

import java.time.LocalDateTime;

public record ConsentimientoVeterinarioResponse(
        String uuid, Long mascotaId, String mascota, Long consultaId, Long apoderadoId,
        String titular, String procedimiento, String diagnosticoIndicacion,
        String descripcionProcedimiento, String beneficiosEsperados, String riesgosPrevisibles,
        String alternativas, String consecuenciasNoRealizar, boolean autorizacionAnestesia,
        boolean autorizacionEmergencia, int version, String contenido, String contenidoHash,
        EstadoConsentimientoVeterinario estado, LocalDateTime creadoAt, LocalDateTime decididoAt,
        String nombreFirmante, String documentoFirmante, String relacionFirmante,
        Boolean preguntasAclaradas, String motivoRechazo, LocalDateTime revocadoAt,
        String motivoRevocacion) {
}
