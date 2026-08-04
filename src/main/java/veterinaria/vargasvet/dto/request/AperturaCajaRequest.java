package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AperturaCajaRequest {
    @NotNull
    private Integer companyId;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("10000.00")
    private BigDecimal montoApertura;
}
