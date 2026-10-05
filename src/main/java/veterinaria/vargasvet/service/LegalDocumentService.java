package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.dto.response.LegalAcceptanceDTO;
import veterinaria.vargasvet.dto.response.LegalDocumentDTO;
import veterinaria.vargasvet.dto.response.LegalStatusDTO;

import java.util.List;

public interface LegalDocumentService {

    List<LegalDocumentDTO> getActiveDocuments();

    LegalStatusDTO getStatus(Integer usuarioId);

    /** Registra la aceptación de la versión VIGENTE de cada documento; una versión retirada se rechaza. */
    void accept(Integer usuarioId, List<Long> legalDocumentIds, String ipAddress, String userAgent);

    /** Publica una versión nueva (inmutable) y retira la vigente; todas las personas deben aceptarla. */
    LegalDocumentDTO publish(LegalDocumentType tipo, String version, String contenido);

    /** Las aceptaciones de la propia persona, la más reciente primero. */
    List<LegalAcceptanceDTO> getMyAcceptances(Integer usuarioId);

    boolean hasPendingConsent(Integer usuarioId);

    /**
     * true cuando el usuario tiene un documento pendiente cuyo período de gracia
     * (legal.grace-period-days desde su vigenteDesde) ya venció.
     */
    boolean isPastGracePeriod(Integer usuarioId);
}
