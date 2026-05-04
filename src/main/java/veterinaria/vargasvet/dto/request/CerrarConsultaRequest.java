package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CerrarConsultaRequest {

    @NotNull(message = "El campo version es requerido para evitar ediciones simultáneas")
    private Long version;
}
