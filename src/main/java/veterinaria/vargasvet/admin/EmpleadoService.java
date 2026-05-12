package veterinaria.vargasvet.admin;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.admin.EmpleadoRequest;
import veterinaria.vargasvet.admin.EmpleadoListResponse;
import veterinaria.vargasvet.admin.HorarioEmpleadoResponse;
import veterinaria.vargasvet.admin.UserProfileDTO;

import java.util.List;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRequest dto);
    UserProfileDTO updateEmpleado(Long empleadoId, EmpleadoRequest dto);
    void cambiarEstado(Long empleadoId, Boolean nuevoEstado);
    void eliminar(Long empleadoId);
    Page<EmpleadoListResponse> listar(Integer companyId, String nombre, String apellido, String email, Long tipoEmpleadoId, Long especialidadId, int page, int size);
    EmpleadoRequest findById(Long id);
    List<HorarioEmpleadoResponse> getHorario(Long empleadoId);
}
