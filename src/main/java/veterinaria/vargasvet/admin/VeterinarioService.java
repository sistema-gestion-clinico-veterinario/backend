package veterinaria.vargasvet.admin;

import veterinaria.vargasvet.auth.VeterinarioRegistrationDTO;
import veterinaria.vargasvet.admin.UserProfileDTO;

public interface VeterinarioService {
    UserProfileDTO registerVeterinario(VeterinarioRegistrationDTO registrationDTO);
}
