package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;

import java.math.BigDecimal;

@Data
public class PagoRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    /** Importe que se aplicará al saldo. Si se omite, se cobra el saldo completo. */
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a 0")
    @DecimalMax(value = "50000.00", message = "El monto a pagar no debe superar S/ 50,000.00")
    private BigDecimal monto;

    @DecimalMin(value = "0.01", message = "El monto recibido debe ser mayor a 0")
    @DecimalMax(value = "10000.00", message = "El efectivo recibido no debe superar S/ 10,000.00")
    private BigDecimal montoRecibido;

    @AssertTrue(message = "El monto recibido es obligatorio para pagos en efectivo")
    public boolean isMontoEfectivoValido() {
        return metodoPago != MetodoPago.EFECTIVO || montoRecibido != null;
    }
}
