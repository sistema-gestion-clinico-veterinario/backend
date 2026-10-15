package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevocarConsentimientoVeterinarioRequest(
        @NotBlank @Size(min = 10, max = 4000) String motivo) {
}
