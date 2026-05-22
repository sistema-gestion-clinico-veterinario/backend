package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RolVentanaPermisoDTO {
    private Integer ventanaId;
    private String  codigo;
    private String  nombre;
    private String  icono;
    private String  ruta;
    private Integer parentId;
    private String  parentCodigo;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
    private List<VistaDTO> vistas;
}

