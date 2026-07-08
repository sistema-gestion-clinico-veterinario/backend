package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;

import java.math.BigDecimal;

@Data
public class PagoRequest {

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @DecimalMin(value = "0.01", message = "El monto recibido debe ser mayor a 0")
    private BigDecimal montoRecibido;

    /** Teléfono Yape del cliente (9 dígitos, sin +51) — solo para MetodoPago.YAPE */
    @Min(value = 900000000L, message = "El telefono Yape debe tener 9 digitos")
    @Max(value = 999999999L, message = "El telefono Yape debe tener 9 digitos")
    private Long yapePhoneNumber;

    /** Código OTP de 6 dígitos generado en la app Yape — solo para MetodoPago.YAPE */
    @Min(value = 100000, message = "El codigo Yape debe tener 6 digitos")
    @Max(value = 999999, message = "El codigo Yape debe tener 6 digitos")
    private Integer yapeOtp;

    /** Email del pagador — requerido por MercadoPago para pagos Yape */
    @Email(message = "El formato del correo del pagador es invalido")
    @Size(max = 100, message = "El correo del pagador no debe superar 100 caracteres")
    @Pattern(regexp = "^$|^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$", message = "El correo del pagador debe estar en minusculas y tener un formato valido")
    private String payerEmail;

    @AssertTrue(message = "Para pagar con Yape debe ingresar telefono, codigo OTP y correo del pagador")
    public boolean isDatosYapeCompletos() {
        return metodoPago != MetodoPago.YAPE || (yapePhoneNumber != null && yapeOtp != null && payerEmail != null && !payerEmail.isBlank());
    }

    @AssertTrue(message = "El monto recibido es obligatorio para pagos en efectivo")
    public boolean isMontoEfectivoValido() {
        return metodoPago != MetodoPago.EFECTIVO || montoRecibido != null;
    }
}
