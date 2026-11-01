package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VincularPuntoCobroRequest(
        @NotBlank(message = "Indica un nombre para identificar este equipo")
        @Size(min = 2, max = 80, message = "El nombre del equipo debe tener entre 2 y 80 caracteres")
        String nombreEquipo) {
}
