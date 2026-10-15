package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import veterinaria.vargasvet.domain.enums.TipoDerechoDatos;

public record CrearSolicitudDerechoRequest(
        @NotNull TipoDerechoDatos tipo,
        @NotBlank @Size(min = 10, max = 4000) String descripcion,
        @Size(max = 4000) String datoRectificar) {
}
