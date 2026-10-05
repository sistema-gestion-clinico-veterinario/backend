package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoInactividad;

@Data
public class AccountStatusRequest {
    /** Al desactivar: suspensión (temporal) o baja. Si no se indica, se asume baja. */
    private TipoInactividad tipo;

    @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
    private String reason;
}
