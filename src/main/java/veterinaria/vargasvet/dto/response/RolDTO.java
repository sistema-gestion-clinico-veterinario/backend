package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Data
public class RolDTO {
    private Integer id;
    private String name;
    private String descripcion;
    private Boolean activo;
    private Integer companyId;
    private RoleScope scope;
    private RolePurpose purpose;
    private boolean systemManaged;
    private boolean protectedRole;
    private long permissionVersion;
}
