package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;

import java.math.BigDecimal;

@Data
public class DetalleCuentaRequest {
    @NotNull(message = "El tipo de concepto es obligatorio")
    private TipoDetalleCuenta tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 180, message = "La descripción no debe superar 180 caracteres")
    private String descripcion;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    @Max(value = 999, message = "La cantidad máxima es 999")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal precioUnitario;
}
