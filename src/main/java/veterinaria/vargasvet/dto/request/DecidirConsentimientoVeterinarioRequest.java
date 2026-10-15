package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecidirConsentimientoVeterinarioRequest(
        @NotNull Boolean aceptar,
        @NotBlank @Size(max = 200) String nombreFirmante,
        @NotBlank @Size(max = 40) String documentoFirmante,
        @NotNull Boolean preguntasAclaradas,
        @Size(max = 4000) String motivoRechazo) {
}
