package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class MenuDTO {
    private Integer id;
    private String label;
    private String icon;
    private String path;
    private List<MenuDTO> children;
}
