package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface UsuarioService {
    UserProfileDTO register(UserRegistrationDTO registrationDTO);
    AuthResponse login(LoginDTO loginDTO);
    UserProfileDTO getProfile(Integer id);
    void suspendAccount(Integer id);
    void verifyEmail(String token);
    void resendVerificationToken(String email);
    void changePassword(String email, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto);
    void resetPassword(veterinaria.vargasvet.dto.request.AdminChangePasswordRequest dto);
    void forgotPassword(veterinaria.vargasvet.dto.request.ForgotPasswordRequest request);
    void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request);
    boolean validateResetToken(String token);
    AuthResponse refreshToken(String refreshToken);
    AuthResponse switchRole(String email, String roleName);
}
