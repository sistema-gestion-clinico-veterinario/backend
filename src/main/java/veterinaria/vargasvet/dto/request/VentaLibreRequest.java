package veterinaria.vargasvet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VentaLibreRequest {

    private Integer companyId;

    private Long apoderadoId;

    @Size(max = 160, message = "El nombre del cliente no debe superar 160 caracteres")
    private String clienteNombre;

    @NotEmpty(message = "Debe agregar al menos un producto")
    @Valid
    private List<VentaLibreItemRequest> items;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @DecimalMin(value = "0.01", message = "El monto recibido debe ser mayor a 0")
    @DecimalMax(value = "10000.00", message = "El efectivo recibido no debe superar S/ 10,000.00")
    private BigDecimal montoRecibido;

    @AssertTrue(message = "El monto recibido es obligatorio para pagos en efectivo")
    public boolean isMontoEfectivoValido() {
        return metodoPago != MetodoPago.EFECTIVO || montoRecibido != null;
    }
}
