package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class RoleCreateDTO {
    @NotBlank(message = "El nombre del rol es obligatorio")
    private String name;

    @NotEmpty(message = "El rol debe tener al menos un permiso")
    private Set<Integer> permissionIds;
}
