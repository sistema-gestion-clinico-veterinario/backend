package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class RolVentanaPermisoDTO {
    private Integer ventanaId;
    private String  codigo;
    private String  nombre;
    private String  icono;
    private Integer parentId;
    private String  parentCodigo;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
}
