package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;

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
