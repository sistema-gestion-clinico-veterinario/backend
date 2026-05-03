package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface VeterinarioService {
    UserProfileDTO registerVeterinario(VeterinarioRegistrationDTO registrationDTO);
}
