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
    private String icono;
    private String ruta;
    private List<MenuItemDTO> hijos;
    private List<VistaDTO> vistas;
    private boolean activo;
    private boolean leer;
    private boolean escribir;
    private boolean modificar;
    private boolean eliminar;
}
