package veterinaria.vargasvet.dto.response;

import java.time.LocalDateTime;

public record DocumentoCentroLegalResponse(String codigo, String titulo, String version,
                                            LocalDateTime vigenteDesde, String contenido) {
}
