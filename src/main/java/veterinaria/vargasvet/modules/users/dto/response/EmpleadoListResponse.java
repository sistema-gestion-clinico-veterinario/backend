package veterinaria.vargasvet.modules.users.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class EmpleadoListResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String numeroColegiatura;
    private String fotoUrl;
    private boolean activo;
    private List<String> tiposEmpleado;
    private List<String> especialidades;
}
