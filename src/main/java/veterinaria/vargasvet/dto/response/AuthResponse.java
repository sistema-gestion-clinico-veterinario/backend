package veterinaria.vargasvet.dto.response;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Data
public class AuthResponse {
    @JsonIgnore
    private String token;
    @JsonIgnore
    private String refreshToken;
    private List<String> roles;
    private List<String> assignedRoles;
    private Integer companyId;
    private String companyName;
    private String nombreCompleto;
    private String userType;
    private Integer empleadoId;
    private boolean passwordChanged;
    private boolean needsCompanySelection;
    private List<Object> menu;
    private List<String> permissions;
}
