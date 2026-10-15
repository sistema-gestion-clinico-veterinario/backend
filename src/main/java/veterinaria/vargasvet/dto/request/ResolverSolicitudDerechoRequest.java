package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;

public record ResolverSolicitudDerechoRequest(
        @NotNull EstadoSolicitudDerecho estado,
        @Size(max = 8000) String respuesta,
        @Size(max = 4000) String motivoRechazo) {
}
