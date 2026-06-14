package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.ConceptoMovimiento;
import veterinaria.vargasvet.domain.enums.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MovimientoCajaResponse {

    private Long id;
    private TipoMovimiento tipo;
    private ConceptoMovimiento concepto;
    private BigDecimal monto;
    private Long citaId;
    private String descripcion;
    private LocalDateTime fecha;
    private String registradoPor;
    private Integer companyId;
}
