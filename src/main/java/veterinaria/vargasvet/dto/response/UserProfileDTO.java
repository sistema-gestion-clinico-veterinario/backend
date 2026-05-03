package veterinaria.vargasvet.dto.response;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Integer id;
    private String email;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String direccion;
    private String systemRole;
    private Integer companyId;
    private String companyName;
}
