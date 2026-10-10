package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.domain.enums.TipoConstanciaLegal;

import java.time.LocalDateTime;

/** Una constancia propia: aceptación contractual o lectura informativa, con su versión y evidencia. */
public record LegalAcceptanceDTO(
        LegalDocumentType tipo,
        String version,
        String contenidoHash,
        boolean textoRecuperable,
        LocalDateTime fechaAceptacion,
        TipoConstanciaLegal tipoConstancia) {
}
