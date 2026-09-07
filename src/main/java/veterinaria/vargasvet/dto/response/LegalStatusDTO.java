package veterinaria.vargasvet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalStatusDTO {
    private boolean needsAcceptance;
    private boolean overdue;
    private List<LegalDocumentDTO> pendingDocuments;
}
