package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClinicalFieldAnswerResponse {
    Long fieldId;
    String conceptCode;
    String label;
    String value;
}
