package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Value
@Builder
public class AssignedRoleResponse {
    Integer id;
    String name;
    RoleScope scope;
    RolePurpose purpose;
}
