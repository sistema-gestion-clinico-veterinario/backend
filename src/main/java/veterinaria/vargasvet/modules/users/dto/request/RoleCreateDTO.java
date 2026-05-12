package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class RoleCreateDTO {
    @NotBlank(message = "El nombre del rol es obligatorio")
    private String name;
    private Set<Integer> permissionIds;
}
