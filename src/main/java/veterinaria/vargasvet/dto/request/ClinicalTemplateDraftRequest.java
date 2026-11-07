package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClinicalTemplateDraftRequest {
    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 160, message = "El nombre no debe superar 160 caracteres")
    private String name;

    @Valid
    @NotEmpty(message = "La plantilla debe tener al menos una sección")
    private List<Section> sections = new ArrayList<>();

    @Data
    public static class Section {
        @NotBlank(message = "El título de la sección es obligatorio")
        @Size(max = 160, message = "El título no debe superar 160 caracteres")
        private String title;

        @NotNull(message = "El orden de la sección es obligatorio")
        private Integer order;

        @Valid
        private List<Field> fields = new ArrayList<>();
    }

    @Data
    public static class Field {
        @NotNull(message = "El concepto del campo es obligatorio")
        private Long conceptId;

        @NotBlank(message = "La etiqueta del campo es obligatoria")
        @Size(max = 160, message = "La etiqueta no debe superar 160 caracteres")
        private String label;

        @Size(max = 500, message = "El texto de ayuda no debe superar 500 caracteres")
        private String helpText;

        private boolean required;
        private boolean visible = true;

        @NotNull(message = "El orden del campo es obligatorio")
        private Integer order;

        private List<String> species = new ArrayList<>();
        private String visibilityRules = "{}";
    }
}
