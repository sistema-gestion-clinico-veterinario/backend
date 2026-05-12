package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;

@Data
public class PermissionDTO {
    private Integer id;
    private String name;
    private String label;
    private String module;
}
