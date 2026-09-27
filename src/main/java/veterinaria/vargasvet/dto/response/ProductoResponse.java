package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProductoResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private String nombre;
    private Long categoriaId;
    private String categoriaNombre;
    private BigDecimal precio;
    private BigDecimal costo;
    private String marca;
    private Integer stock;
    private Integer stockMinimo;
    private String descripcion;
    private String imagenUrl;
    private String sku;
    private String codigoBarras;
    private LocalDate fechaVencimiento;
    private Boolean requiereReceta;
    private Long unidadMedidaId;
    private String unidadMedidaNombre;
    private LocalDate proximoVencimientoLote;
    private Boolean activo;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
