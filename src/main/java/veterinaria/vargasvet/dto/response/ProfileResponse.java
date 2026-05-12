package veterinaria.vargasvet.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProfileResponse {
    private Integer id;
    private String email;
    private String nombre;
    private String apellido;
    private String dni;
    private String telefono;
    private String direccion;
    private Set<String> roles;
    private String companyName;
    private Boolean activo;

    private Long empleadoId;
    private String genero;
    private String tipoDocumento;
    private String numeroColegiatura;
    private String observaciones;
    private String fotoUrl;
    private Set<String> especialidades;
    private Set<String> tiposEmpleado;
    private List<HorarioEmpleadoResponse> horarios;
    private boolean empleado;
}
