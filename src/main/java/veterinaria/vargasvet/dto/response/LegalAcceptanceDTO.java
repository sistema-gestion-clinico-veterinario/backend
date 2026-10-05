package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.LegalDocumentType;

import java.time.LocalDateTime;

/** Una aceptación propia: qué documento y versión, cuándo, y si el texto exacto se puede demostrar. */
public record LegalAcceptanceDTO(
        LegalDocumentType tipo,
        String version,
        String contenidoHash,
        boolean textoRecuperable,
        LocalDateTime fechaAceptacion) {
}
