package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.EmpleadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRequest dto);
    UserProfileDTO updateEmpleado(Integer usuarioId, EmpleadoRequest dto);
    void cambiarEstado(Integer usuarioId, Boolean nuevoEstado);
}
