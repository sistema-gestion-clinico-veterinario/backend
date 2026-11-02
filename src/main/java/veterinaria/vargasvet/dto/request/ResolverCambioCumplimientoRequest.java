package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverCambioCumplimientoRequest(
        boolean aprobar,
        @NotBlank @Size(min = 5, max = 2000) String comentario,
        boolean confirmoRevisionLegal) {
}
