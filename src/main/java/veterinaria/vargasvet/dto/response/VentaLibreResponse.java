package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaLibreResponse {
    private Long id;
    private String numeroVenta;
    private String clienteNombre;
    private List<VentaLibreDetalleResponse> items;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private LocalDateTime fecha;
}
