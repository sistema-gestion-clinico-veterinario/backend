package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.dto.request.CamposAvisoPrivacidad;

import java.time.LocalDateTime;

public record AvisoPrivacidadResponse(Integer version, String contenido, String contenidoHash,
                                      LocalDateTime vigenteDesde, boolean activo, String creadoPor,
                                      CamposAvisoPrivacidad campos) {
}
