package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

import java.time.LocalDateTime;

public record AvisoPublicoResponse(String clinica, String logoUrl, String colorPrimario,
                                   AudienciaAvisoPrivacidad audiencia, Integer version,
                                   LocalDateTime vigenteDesde, String contenido) {
}
