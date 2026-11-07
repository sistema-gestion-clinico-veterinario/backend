package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.ClinicalValueType;

@Data
public class ClinicalConceptRequest {
    @Size(max = 80, message = "El código no debe superar 80 caracteres")
    @Pattern(regexp = "^$|[a-z][a-z0-9_]{2,79}$", message = "El código debe usar letras minúsculas, números y guion bajo")
    private String code;

    @NotBlank(message = "El nombre del campo es obligatorio")
    @Size(max = 160, message = "El nombre no debe superar 160 caracteres")
    private String name;

    @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
    private String description;

    @NotNull(message = "El tipo de dato es obligatorio")
    private ClinicalValueType valueType;

    @Size(max = 40, message = "La unidad no debe superar 40 caracteres")
    private String unit;

    private boolean reportable;
    private String validationConfig = "{}";
}
