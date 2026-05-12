package veterinaria.vargasvet.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PermissionDTO {
    private Integer id;
    private String name;
    private String label;
    private String description;
    private String module;
}
