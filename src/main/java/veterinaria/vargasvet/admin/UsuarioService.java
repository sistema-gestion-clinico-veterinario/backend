package veterinaria.vargasvet.admin;

import veterinaria.vargasvet.auth.LoginDTO;
import veterinaria.vargasvet.auth.UserRegistrationDTO;
import veterinaria.vargasvet.auth.AuthResponse;
import veterinaria.vargasvet.admin.UserProfileDTO;

public interface UsuarioService {
    UserProfileDTO register(UserRegistrationDTO registrationDTO);
    AuthResponse login(LoginDTO loginDTO);
    UserProfileDTO getProfile(Integer id);
    void suspendAccount(Integer id);
    void verifyEmail(String token);
    void resendVerificationToken(String email);
    void changePassword(String email, veterinaria.vargasvet.auth.ChangePasswordDTO dto);
    void resetPassword(veterinaria.vargasvet.auth.AdminChangePasswordRequest dto);
}
