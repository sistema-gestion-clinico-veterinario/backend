package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ClinicalTemplateSummaryResponse {
    Long id;
    String templateKey;
    String name;
    Integer version;
    ClinicalTemplateStatus status;
    LocalDateTime publishedAt;
    String description;
    boolean defaultTemplate;
    boolean active;
    List<String> species;
    List<Long> serviceIds;
    List<String> serviceNames;
}
