package veterinaria.vargasvet.admin;

import lombok.Data;
import java.util.Set;

@Data
public class RoleDTO {
    private Integer id;
    private String name;
    private Set<PermissionDTO> permissions;
}
