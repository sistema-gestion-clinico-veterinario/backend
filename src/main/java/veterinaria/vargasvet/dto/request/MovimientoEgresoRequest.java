package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import veterinaria.vargasvet.domain.enums.ConceptoMovimiento;

import java.math.BigDecimal;

@Data
public class MovimientoEgresoRequest {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal monto;

    @NotBlank
    @Size(max = 300)
    private String descripcion;

    @NotNull
    private Integer companyId;

    private ConceptoMovimiento concepto = ConceptoMovimiento.GASTO_OPERATIVO;
}
