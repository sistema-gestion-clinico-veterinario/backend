package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.TipoAplicacionProducto;
import veterinaria.vargasvet.domain.enums.TipoControlStock;

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
    private Long marcaId;
    private Integer stock;
    private TipoControlStock controlStock;
    private Integer stockMinimo;
    private String descripcion;
    private String imagenUrl;
    private String sku;
    private String codigoBarras;
    private LocalDate fechaVencimiento;
    private Boolean requiereReceta;
    private Long unidadMedidaId;
    private String unidadMedidaNombre;
    private TipoAplicacionProducto aplicacionEspecie;
    private Set<EspecieMascota> especies;
    private LocalDate proximoVencimientoLote;
    private Boolean activo;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
