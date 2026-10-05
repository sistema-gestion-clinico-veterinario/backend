package veterinaria.vargasvet.dto.response;

import java.time.LocalDateTime;

public record AvisoPublicoResponse(String clinica, String logoUrl, String colorPrimario, Integer version,
                                   LocalDateTime vigenteDesde, String contenido) {
}
