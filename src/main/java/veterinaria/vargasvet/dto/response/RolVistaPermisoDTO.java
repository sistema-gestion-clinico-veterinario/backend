package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.ViewAudience;
import veterinaria.vargasvet.domain.enums.DataScope;

@Data
public class RolVistaPermisoDTO {
    private Integer vistaId;
    private String codigo;
    private String nombre;
    private String ruta;
    private String grupo;
    private Integer ventanaId;
    private String ventanaCodigo;
    private String ventanaNombre;
    private Integer orden;
    private ViewAudience audience;
    private boolean visibleMenu;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
    private DataScope dataScope = DataScope.OWN;
}
