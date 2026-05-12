package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.EmpleadoListResponse;
import veterinaria.vargasvet.dto.response.HorarioEmpleadoResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

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
