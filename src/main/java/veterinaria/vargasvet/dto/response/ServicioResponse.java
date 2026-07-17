package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import veterinaria.vargasvet.domain.enums.TipoControlServicio;

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
    private Integer duracionEstimada;
    private Boolean permiteEmergencia;
    private Long tipoEmpleadoId;
    private String tipoEmpleadoNombre;
    private TipoControlServicio tipoControlPreventivo;
}
