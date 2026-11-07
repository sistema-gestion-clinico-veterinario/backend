package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;

import java.time.LocalDateTime;

@Value
@Builder
public class ClinicalTemplateSummaryResponse {
    Long id;
    String templateKey;
    String name;
    Integer version;
    ClinicalTemplateStatus status;
    LocalDateTime publishedAt;
}
