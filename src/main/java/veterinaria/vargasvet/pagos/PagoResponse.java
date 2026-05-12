package veterinaria.vargasvet.pagos;

import lombok.Data;
import veterinaria.vargasvet.shared.MetodoPago;
import veterinaria.vargasvet.shared.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoResponse {
    private Long id;
    private Long citaId;
    private MetodoPago metodoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private LocalDateTime fechaPago;
    private PaymentStatus estado;
}
