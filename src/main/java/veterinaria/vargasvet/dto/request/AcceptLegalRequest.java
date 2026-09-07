package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AcceptLegalRequest {
    @NotEmpty
    private List<Long> legalDocumentIds;
}
