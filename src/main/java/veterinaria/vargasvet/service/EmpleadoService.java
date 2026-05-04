package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.EmpleadoListResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRequest dto);
    UserProfileDTO updateEmpleado(Integer usuarioId, EmpleadoRequest dto);
    void cambiarEstado(Integer usuarioId, Boolean nuevoEstado);
    Page<EmpleadoListResponse> listar(Integer companyId, String nombre, Long tipoEmpleadoId, Long especialidadId, int page, int size);
}
