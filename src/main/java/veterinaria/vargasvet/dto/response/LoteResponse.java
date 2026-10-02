package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoteResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private LocalDate fechaIngreso;
    private Integer cantidad;
    private Integer cantidadInicial;
    private BigDecimal costoUnitario;
    private Boolean activo;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
