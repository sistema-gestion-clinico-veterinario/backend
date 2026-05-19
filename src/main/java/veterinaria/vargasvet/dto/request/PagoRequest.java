package veterinaria.vargasvet.dto.request;

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

    private BigDecimal montoRecibido;

    /** Teléfono Yape del cliente (9 dígitos, sin +51) — solo para MetodoPago.YAPE */
    private Long yapePhoneNumber;

    /** Código OTP de 6 dígitos generado en la app Yape — solo para MetodoPago.YAPE */
    private Integer yapeOtp;

    /** Email del pagador — requerido por MercadoPago para pagos Yape */
    private String payerEmail;
}
