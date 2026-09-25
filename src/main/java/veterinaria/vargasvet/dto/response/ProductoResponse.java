package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private String nombre;
    private Long categoriaId;
    private String categoriaNombre;
    private BigDecimal precio;
    private Integer stock;
    private String descripcion;
    private String imagenUrl;
    private Boolean activo;
}
