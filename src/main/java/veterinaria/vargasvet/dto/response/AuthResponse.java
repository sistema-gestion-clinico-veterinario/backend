package veterinaria.vargasvet.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String token;
    private String systemRole;
    private Integer companyId;
    private String companyName;
    private List<String> permissions;
    private String nombreCompleto;
    private String userType;
    private boolean needsCompanySelection;
}
