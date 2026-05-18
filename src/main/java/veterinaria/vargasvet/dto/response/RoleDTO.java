package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.util.Set;

@Data
public class RoleDTO {
    private Integer id;
    private String name;
    private Integer companyId;
    private Set<PermissionDTO> permissions;
    private Set<Integer> menuIds;
}
