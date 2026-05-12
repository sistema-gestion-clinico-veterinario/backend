package veterinaria.vargasvet.modules.users.service;

import veterinaria.vargasvet.modules.users.dto.request.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;

public interface VeterinarioService {
    UserProfileDTO registerVeterinario(VeterinarioRegistrationDTO registrationDTO);
}
