package veterinaria.vargasvet.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.request.SwitchRoleRequest;
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

    @Value("${jwt.validity-in-seconds:1800}")
    private long accessTokenMaxAge;

    @Value("${jwt.refresh-validity-in-seconds:604800}")
    private long refreshTokenMaxAge;

    @PostConstruct
    void validateCookieConfiguration() {
        if (!java.util.Set.of("Lax", "Strict", "None").contains(cookieSameSite)) {
            throw new IllegalStateException("COOKIE_SAME_SITE debe ser Lax, Strict o None");
        }
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalStateException("Las cookies SameSite=None requieren COOKIE_SECURE=true");
        }
    }

    @PostMapping("/register")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
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

    @PostMapping("/setup-account")
    public ResponseEntity<ApiResponse<Void>> setupAccount(@Valid @RequestBody veterinaria.vargasvet.dto.request.SetupAccountRequest request) {
        usuarioService.setupAccount(request.getToken(), request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta activada y contraseña creada exitosamente", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        usuarioService.resendVerificationToken(email);
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Si la cuenta requiere verificación, se enviaron las instrucciones.", null));
    }

    @GetMapping("/profile/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_EMPLEADOS', 'LEER')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(@PathVariable Integer id) {
        UserProfileDTO profile = usuarioService.getProfile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    @PutMapping("/suspend/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_EMPLEADOS', 'MODIFICAR')")
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
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
                                                             @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                                             HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.refreshToken(refreshTokenCookie);
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refrescado exitosamente", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie,
                                                      HttpServletResponse httpResponse) {
        usuarioService.revokeRefreshToken(refreshTokenCookie);
        clearAuthCookies(httpResponse);
        return ResponseEntity.ok(new ApiResponse<>(true, "Sesión cerrada exitosamente", null));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<ApiResponse<AuthResponse>> switchRole(@Valid @RequestBody SwitchRoleRequest request,
                                                                HttpServletResponse httpResponse) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        AuthResponse response = usuarioService.switchRole(email, request.getRoleId());
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
        addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, accessTokenMaxAge, "/api/v1");
        addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, refreshTokenMaxAge, "/api/v1/auth");
    }

    private void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE, "", 0, "/api/v1");
        addCookie(response, REFRESH_TOKEN_COOKIE, "", 0, "/api/v1/auth");
    }

    private void addCookie(HttpServletResponse response, String name, String value,
                           long maxAgeSeconds, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(path)
                .maxAge(java.time.Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
