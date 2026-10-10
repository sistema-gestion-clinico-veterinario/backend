package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

import java.time.LocalDateTime;

public record AvisoPrivacidadResponse(AudienciaAvisoPrivacidad audiencia, Integer version, String contenido, String contenidoHash,
                                      LocalDateTime vigenteDesde, boolean activo, String creadoPor,
                                      String creadoDispositivo, String creadoIp, CamposAvisoPrivacidad campos) {
}
