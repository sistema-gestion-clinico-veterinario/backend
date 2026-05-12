package veterinaria.vargasvet.auth;

import lombok.Data;
import veterinaria.vargasvet.admin.MenuDTO;
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
    private Integer empleadoId;
    private boolean passwordChanged;
    private boolean needsCompanySelection;
    private List<MenuDTO> menu;
}
