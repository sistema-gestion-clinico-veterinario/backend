package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;
import veterinaria.vargasvet.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos mínimos de una transacción necesarios para el dashboard. */
@Data
@Builder
public class DashboardPaymentSummary {
    private Long id;
    private MetodoPago metodoPago;
    private BigDecimal monto;
    private LocalDateTime fechaPago;
    private PaymentStatus estado;
    private String mascotaNombre;
    private String servicioNombre;
}
