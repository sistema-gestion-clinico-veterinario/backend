package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.EmpleadoRegistrationDTO;
import veterinaria.vargasvet.dto.request.EmpleadoUpdateDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRegistrationDTO dto);
    UserProfileDTO updateEmpleado(Integer usuarioId, EmpleadoUpdateDTO dto);
}
