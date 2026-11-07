package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.ClinicalConceptOrigin;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;

@Value
@Builder
public class ClinicalConceptResponse {
    Long id;
    String code;
    String name;
    String description;
    ClinicalValueType valueType;
    String unit;
    ClinicalConceptOrigin origin;
    boolean reportable;
    boolean active;
    String validationConfig;
}
