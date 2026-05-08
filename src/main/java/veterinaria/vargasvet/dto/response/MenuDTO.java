package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MenuDTO {
    private Integer id;
    private String label;
    private String icon;
    private String path;
    private List<MenuDTO> children;
}
