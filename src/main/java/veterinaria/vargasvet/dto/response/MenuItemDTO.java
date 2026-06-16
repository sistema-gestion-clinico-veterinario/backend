package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuItemDTO {
    private Integer id;
    private String codigo;
    private String nombre;
    private String ruta;
    private String grupo;
    private Integer orden;
    private Integer ordenGrupo;
    private boolean activo;
    private String icono;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
}
