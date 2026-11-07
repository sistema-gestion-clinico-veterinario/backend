package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClinicalFieldAnswerRequest {
    @NotNull(message = "El campo clínico es obligatorio")
    private Long fieldId;

    /** Valor transportado como texto; el backend lo convierte y valida según el concepto. */
    private String value;
}
