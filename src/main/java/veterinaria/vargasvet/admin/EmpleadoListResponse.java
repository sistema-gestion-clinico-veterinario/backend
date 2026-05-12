package veterinaria.vargasvet.admin;

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
    private Boolean activo;
    private List<String> tiposEmpleado;
    private List<String> especialidades;
}
