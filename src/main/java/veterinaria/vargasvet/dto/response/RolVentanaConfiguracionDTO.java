package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.MenuPresentation;

@Data
public class RolVentanaConfiguracionDTO {
    private Integer ventanaId;
    private String codigo;
    private String nombre;
    private String icono;
    private MenuPresentation presentacion;
    private Integer orden;
}
