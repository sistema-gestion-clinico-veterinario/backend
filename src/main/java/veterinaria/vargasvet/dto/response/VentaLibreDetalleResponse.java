package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VentaLibreDetalleResponse {
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
