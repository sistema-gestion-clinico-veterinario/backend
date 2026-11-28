package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;

@Value
@Builder
@Jacksonized
public class AssignedRoleResponse {
    Integer id;
    String name;
    RoleScope scope;
    RolePurpose purpose;
}
