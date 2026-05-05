package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServicioResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Boolean disponible;
    private Boolean activo;
}
