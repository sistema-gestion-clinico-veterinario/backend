package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArqueoCajaRequest {
    @NotNull
    private Integer companyId;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100000.00")
    private BigDecimal efectivoContado;

    @Size(max = 300)
    private String observaciones;
}
