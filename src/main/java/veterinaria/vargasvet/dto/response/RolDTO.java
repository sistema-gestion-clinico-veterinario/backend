package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class RolDTO {
    private Integer id;
    private String name;
    private String descripcion;
    private Boolean activo;
    private Integer companyId;
}
