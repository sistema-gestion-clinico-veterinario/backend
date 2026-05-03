package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.EmpleadoRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface EmpleadoService {
    UserProfileDTO registerEmpleado(EmpleadoRegistrationDTO dto);
}
