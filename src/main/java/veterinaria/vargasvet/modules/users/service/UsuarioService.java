package veterinaria.vargasvet.modules.users.service;

import veterinaria.vargasvet.modules.users.dto.request.LoginDTO;
import veterinaria.vargasvet.modules.users.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.modules.users.dto.response.AuthResponse;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;

public interface UsuarioService {
    UserProfileDTO register(UserRegistrationDTO registrationDTO);
    AuthResponse login(LoginDTO loginDTO);
    UserProfileDTO getProfile(Integer id);
    void suspendAccount(Integer id);
    void verifyEmail(String token);
    void resendVerificationToken(String email);
    void changePassword(String email, veterinaria.vargasvet.modules.users.dto.request.ChangePasswordDTO dto);
    void resetPassword(veterinaria.vargasvet.modules.users.dto.request.AdminChangePasswordRequest dto);
}
