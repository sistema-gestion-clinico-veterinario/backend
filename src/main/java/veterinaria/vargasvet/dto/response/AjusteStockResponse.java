package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MotivoAjusteStock;

import java.time.LocalDateTime;

@Data
public class AjusteStockResponse {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private String productoSku;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private Integer diferencia;
    private MotivoAjusteStock motivo;
    private String observaciones;
    private LocalDateTime createdAt;
    private String createdBy;
}
