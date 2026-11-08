package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.ClinicalTemplateStatus;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class ClinicalTemplateResponse {
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
    List<Section> sections;

    @Value
    @Builder
    public static class Section {
        Long id;
        String title;
        Integer order;
        List<Field> fields;
    }

    @Value
    @Builder
    public static class Field {
        Long id;
        Long conceptId;
        String conceptCode;
        String label;
        String helpText;
        ClinicalValueType valueType;
        String unit;
        boolean required;
        boolean visible;
        boolean reportable;
        Integer order;
        List<String> species;
        String validationConfig;
        String visibilityRules;
    }
}
