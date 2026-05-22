package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class RolVistaPermisoDTO {
    private Integer vistaId;
    private String codigo;
    private String nombre;
    private String ruta;
    private String grupo;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
}
