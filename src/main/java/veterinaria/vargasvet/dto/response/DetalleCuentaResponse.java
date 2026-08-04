package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.TipoDetalleCuenta;

import java.math.BigDecimal;

@Data
public class DetalleCuentaResponse {
    private Long id;
    private TipoDetalleCuenta tipo;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Boolean esServicioBase;
}
