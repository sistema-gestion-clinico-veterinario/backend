package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import veterinaria.vargasvet.domain.enums.MenuPresentation;

@Data
@Builder
public class MenuStructureDTO {
    private Integer ventanaId;
    private String ventanaCodigo;
    private String ventanaNombre;
    private String ventanaIcono;
    private MenuPresentation presentacion;
    private String grupo;
    private Integer orden;
    private List<MenuItemDTO> vistas;
}
