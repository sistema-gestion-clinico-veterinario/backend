package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;
import veterinaria.vargasvet.domain.enums.ReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoResponse {
    private Long id;
    private Long citaId;
    private String numeroCita;
    private MetodoPago metodoPago;
    private BigDecimal monto;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private BigDecimal saldoPendiente;
    private LocalDateTime fechaPago;
    private PaymentStatus estado;
    private String mercadoPagoId;
    private String mpStatus;
    private String businessTraceId;
    private ReceiptStatus receiptStatus;
    private Integer receiptAttempts;
    private String receiptError;
}
