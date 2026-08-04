package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CuentaCitaResponse {
    private Long citaId;
    private String numeroCita;
    private String mascotaNombre;
    private String apoderadoNombre;
    private String servicioNombre;
    private LocalDateTime fechaAtencion;
    private EstadoCita estadoCita;
    private Integer companyId;
    private BigDecimal total;
    private BigDecimal montoPagado;
    private BigDecimal saldoPendiente;
    private String estadoPago;
    private List<DetalleCuentaResponse> detalles;
}
