package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.modules.pagos.domain.enums.MetodoPago;

import java.math.BigDecimal;

@Data
public class PagoRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    private BigDecimal montoRecibido;
}
