package veterinaria.vargasvet.modules.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuRequestDTO {
    @NotBlank(message = "La etiqueta es obligatoria")
    private String label;
    private String icon;
    private String path;
    private Integer sortOrder;
    private Integer parentId;
    private String requiredPermission;
    private boolean active = true;
}
