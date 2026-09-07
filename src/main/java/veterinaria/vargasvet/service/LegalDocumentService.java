package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;

import java.util.List;

public interface LegalDocumentService {

    List<LegalDocumentDTO> getActiveDocuments();

    LegalStatusDTO getStatus(Integer usuarioId);

    void accept(Integer usuarioId, List<Long> legalDocumentIds, String ipAddress, String userAgent);

    boolean hasPendingConsent(Integer usuarioId);

    /**
     * true cuando el usuario tiene un documento pendiente cuyo período de gracia
     * (legal.grace-period-days desde su vigenteDesde) ya venció.
     */
    boolean isPastGracePeriod(Integer usuarioId);
}
