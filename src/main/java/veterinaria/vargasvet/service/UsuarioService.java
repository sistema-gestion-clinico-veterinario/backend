package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface UsuarioService {
    AuthResponse login(LoginDTO loginDTO);

    /** Ruta reservada para SuperAdmin (PLATFORM_ADMIN) - no pasa por slug de
     * empresa ni chequeo de membresia, ya que supervisa todas las empresas. */
    AuthResponse adminLogin(veterinaria.vargasvet.dto.request.AdminLoginDTO adminLoginDTO);

    /** Login vía "Continuar con Google": el correo ya viene verificado por Google (no
     * requiere contraseña), pero solo funciona para una cuenta YA EXISTENTE con
     * credencial creada en esa empresa - no crea cuentas nuevas. */
    AuthResponse loginWithGoogle(String email, String slug);
    UserProfileDTO getProfile(Integer id);

    void setupAccount(String token, String password, Boolean avisoLeido, String ipAddress, String userAgent);

    /** Activa la cuenta invitada sin contraseña: el correo que Google verificó debe
     * coincidir con el correo del usuario dueño del token (si no, GoogleEmailMismatchException) -
     * no crea contraseña, la persona puede crear una despues desde su perfil o con
     * "olvide mi contraseña" si algun dia necesita login con usuario/contraseña. */
    AuthResponse activateAccountWithGoogle(String token, String googleEmail);
    void resendVerificationToken(String email, String slug);

    /** Reenvía el enlace de activación usando el enlace anterior (aunque haya vencido) como
     * prueba de que la persona recibió la invitación; devuelve el correo enmascarado al que
     * se envió. 404 si el enlace no existe o ya no corresponde a una cuenta pendiente. */
    String resendVerificationByToken(String token, String slug);
    void changePassword(Integer usuarioId, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto);
    void requestPasswordReset(veterinaria.vargasvet.dto.request.AdminPasswordResetRequest dto);
    void forgotPassword(veterinaria.vargasvet.dto.request.ForgotPasswordRequest request);
    void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request);
    boolean validateResetToken(String token);
    AuthResponse refreshToken(String refreshToken);
    AuthResponse switchRole(Integer usuarioId, Integer roleId);
    AuthResponse currentSession(Integer usuarioId);
    void revokeRefreshToken(String refreshToken);
}
