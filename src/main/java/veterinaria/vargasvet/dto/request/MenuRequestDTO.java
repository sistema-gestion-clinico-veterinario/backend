package veterinaria.vargasvet.dto.request;

import lombok.Data;

@Data
public class MenuRequestDTO {
    private String label;
    private String icon;
    private String path;
    private Integer sortOrder;
    private Integer parentId;
    private String requiredPermission;
    private boolean active = true;
}
