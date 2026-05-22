package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VistaDTO {
    private Integer id;
    private String codigo;
    private String nombre;
    private String ruta;
    private boolean activo;
}
