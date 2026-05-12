package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String token;
    private List<String> roles;
    private Integer companyId;
    private String companyName;
    private List<String> permissions;
    private String nombreCompleto;
    private String userType;
    private boolean passwordChanged;
    private Integer empleadoId;
    private List<MenuDTO> menu;
}
