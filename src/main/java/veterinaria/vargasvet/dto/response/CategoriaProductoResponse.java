package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class CategoriaProductoResponse {
    private Long id;
    private Integer companyId;
    private String companyName;
    private String nombre;
    private Boolean activo;
}
