package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PagoPortalResponse {
    private Long id;
    private String tipoItem;
    private String descripcion;
    private Long citaId;
    private String mascotaNombre;
    private String servicioNombre;
    private String veterinarioNombre;
    private BigDecimal total;
    private BigDecimal montoPagado;
    private String estadoPago;
    private String metodoPago;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private String fechaPago;
    private String fecha;
    private String estadoCita;
}
