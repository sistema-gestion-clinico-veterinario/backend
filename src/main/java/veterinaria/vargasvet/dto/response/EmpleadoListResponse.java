package veterinaria.vargasvet.dto.response;

import lombok.Data;
import veterinaria.vargasvet.dto.ProtectedPersonalData;
import veterinaria.vargasvet.util.DataMasking;

import java.util.List;

@Data
public class EmpleadoListResponse implements ProtectedPersonalData {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String numeroColegiatura;
    private String fotoUrl;
    private Boolean activo;
    private Boolean cuentaPendiente;
    private veterinaria.vargasvet.domain.enums.TipoInactividad tipoInactividad;
    private List<String> tiposEmpleado;
    private List<String> especialidades;
    private Integer userId;

    @Override
    public String authorizingView() {
        return "VISTA_EMPLEADOS";
    }

    @Override
    public void maskSensitiveData() {
        email = DataMasking.correo(email);
        telefono = DataMasking.telefono(telefono);
    }
}
