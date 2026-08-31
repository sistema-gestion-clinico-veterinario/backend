package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MenuItemType;

import java.util.ArrayList;
import java.util.List;

@Data
public class RolMenuOrdenItemDTO {
    private MenuItemType tipo;
    private Integer referenciaId;
    private String codigo;
    private String nombre;
    private String icono;
    private Integer orden;
    private List<RolMenuOrdenItemDTO> vistas = new ArrayList<>();
}
