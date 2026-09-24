package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface UsuarioService {
    UserProfileDTO register(UserRegistrationDTO registrationDTO);
    AuthResponse login(LoginDTO loginDTO);

    /** Ruta reservada para SuperAdmin (PLATFORM_ADMIN) - no pasa por slug de
     * empresa ni chequeo de membresia, ya que supervisa todas las empresas. */
    AuthResponse adminLogin(veterinaria.vargasvet.dto.request.AdminLoginDTO adminLoginDTO);

    /** Login vía "Continuar con Google": el correo ya viene verificado por Google (no
     * requiere contraseña), pero solo funciona para una cuenta YA EXISTENTE con
     * credencial creada en esa empresa - no crea cuentas nuevas. */
    AuthResponse loginWithGoogle(String email, String slug);
    UserProfileDTO getProfile(Integer id);
    void suspendAccount(Integer id);

    /** Un admin de la empresa fuerza el cambio de correo de otra cuenta, sin pasar por
     * la doble confirmación (correo actual + nuevo) del autoservicio - pensado para
     * cuando la persona perdió el acceso a su correo anterior (que es justo el motivo
     * más común para querer cambiarlo) y por eso nunca podría confirmarlo. Requiere que
     * quien ejecuta la acción ya esté autenticado como admin de esa empresa. */
    void adminChangeEmail(Integer targetUserId, String newEmail);
    void verifyEmail(String token);
    void setupAccount(String token, String password);
    void resendVerificationToken(String email);
    void changePassword(Integer usuarioId, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto);
    void requestPasswordReset(veterinaria.vargasvet.dto.request.AdminPasswordResetRequest dto);
    void forgotPassword(veterinaria.vargasvet.dto.request.ForgotPasswordRequest request);
    void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request);
    boolean validateResetToken(String token);
    AuthResponse refreshToken(String refreshToken);
    AuthResponse switchRole(Integer usuarioId, Integer roleId);
    void revokeRefreshToken(String refreshToken);
}
