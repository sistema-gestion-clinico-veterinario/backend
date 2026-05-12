package veterinaria.vargasvet.modules.users.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.users.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.modules.users.dto.response.EmpleadoListResponse;
import veterinaria.vargasvet.modules.users.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;

import java.util.List;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRequest dto);
    UserProfileDTO updateEmpleado(Long id, EmpleadoRequest dto);
    void cambiarEstado(Long id, Boolean active);
    Page<EmpleadoListResponse> listar(Integer companyId, String nombre, String apellido, String email, Long tipoEmpleadoId, Long especialidadId, int page, int size);
    List<HorarioEmpleadoResponse> getHorario(Long empleadoId);
    EmpleadoRequest findById(Long id);
}
