package veterinaria.vargasvet.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.service.UsuarioService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UsuarioService usuarioService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileDTO>> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserProfileDTO profile = usuarioService.register(registrationDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Usuario registrado exitosamente", profile));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginDTO loginDTO,
                                                           HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.login(loginDTO);
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String token) {
        usuarioService.verifyEmail(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Correo verificado exitosamente. Ya puedes iniciar sesión.", null));
    }

    @PostMapping("/setup-account")
    public ResponseEntity<ApiResponse<Void>> setupAccount(@Valid @RequestBody veterinaria.vargasvet.dto.request.SetupAccountRequest request) {
        usuarioService.setupAccount(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta activada y contraseña creada exitosamente", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        usuarioService.resendVerificationToken(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Correo de verificación reenviado exitosamente.", null));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(@PathVariable Integer id) {
        UserProfileDTO profile = usuarioService.getProfile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    @PutMapping("/suspend/{id}")
    public ResponseEntity<ApiResponse<Void>> suspendAccount(@PathVariable Integer id) {
        usuarioService.suspendAccount(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta suspendida", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        usuarioService.changePassword(email, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada exitosamente", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody(required = false) java.util.Map<String, String> request,
                                                             @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                                             HttpServletResponse httpResponse) {
        String refreshToken = refreshTokenCookie;
        if (refreshToken == null && request != null) {
            refreshToken = request.get("refreshToken");
        }
        AuthResponse response = usuarioService.refreshToken(refreshToken);
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refrescado exitosamente", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpResponse) {
        clearAuthCookies(httpResponse);
        return ResponseEntity.ok(new ApiResponse<>(true, "Sesión cerrada exitosamente", null));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<ApiResponse<AuthResponse>> switchRole(@RequestBody java.util.Map<String, String> request,
                                                                HttpServletResponse httpResponse) {
        String roleName = request.get("roleName");
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        AuthResponse response = usuarioService.switchRole(email, roleName);
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol cambiado exitosamente", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ForgotPasswordRequest request) {
        usuarioService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Si el correo existe, se han enviado las instrucciones para restablecer la contraseña.", null));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(@RequestParam String token) {
        boolean isValid = usuarioService.validateResetToken(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token validado", isValid));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ResetPasswordRequest request) {
        usuarioService.resetPasswordWithToken(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña restablecida exitosamente", null));
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, 1800);
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, 604800);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(cookie);
    }
}
