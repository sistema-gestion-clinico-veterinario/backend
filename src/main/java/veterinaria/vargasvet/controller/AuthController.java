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
import veterinaria.vargasvet.service.impl.GoogleOAuthService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UsuarioService usuarioService;
    private final veterinaria.vargasvet.service.EmailChangeService emailChangeService;
    private final GoogleOAuthService googleOAuthService;
    private final GoogleLoginExchangeStore googleLoginExchangeStore;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${jwt.validity-in-seconds:1800}")
    private long accessTokenMaxAge;

    @Value("${jwt.refresh-validity-in-seconds:604800}")
    private long refreshTokenMaxAge;

    @Value("${app.url}")
    private String frontendUrl;

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

    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AdminLoginDTO adminLoginDTO,
            HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.adminLogin(adminLoginDTO);
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    /** Prefijo del "state" cuando el redirect a Google viene de la pantalla de activacion
     * de cuenta (no de login) - el unico canal que Google devuelve intacto es "state", asi
     * que se usa para distinguir los dos casos y cargar el token de verificacion. */
    private static final String GOOGLE_ACTIVATION_STATE_PREFIX = "activate:";

    /** Adonde Google redirige al navegador tras el consentimiento. No es una llamada del
     * frontend (fetch/XHR) sino una navegación real del navegador, así que nunca devuelve
     * JSON - siempre redirige (éxito o error) de vuelta al frontend. */
    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        boolean isActivation = state != null && state.startsWith(GOOGLE_ACTIVATION_STATE_PREFIX);
        String activationToken = isActivation ? state.substring(GOOGLE_ACTIVATION_STATE_PREFIX.length()) : null;

        String redirectTarget;
        if (error != null || code == null || code.isBlank()) {
            redirectTarget = isActivation
                    ? frontendVerifyUrl(activationToken, "google_cancelado")
                    : frontendLoginUrl(state, "google_cancelado");
        } else {
            try {
                GoogleOAuthService.GoogleIdentity identity = googleOAuthService.resolveIdentity(code);
                if (!identity.emailVerified()) {
                    redirectTarget = isActivation
                            ? frontendVerifyUrl(activationToken, "google_email_no_verificado")
                            : frontendLoginUrl(state, "google_email_no_verificado");
                } else {
                    AuthResponse response = isActivation
                            ? usuarioService.activateAccountWithGoogle(activationToken, identity.email())
                            : usuarioService.loginWithGoogle(identity.email(), state);
                    String exchangeCode = googleLoginExchangeStore.store(response);
                    redirectTarget = frontendUrl + "/auth/google/callback?code="
                            + java.net.URLEncoder.encode(exchangeCode, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (veterinaria.vargasvet.exception.GoogleEmailMismatchException ex) {
                redirectTarget = isActivation
                        ? frontendVerifyUrl(activationToken, "google_correo_no_coincide")
                        : frontendLoginUrl(state, "google_fallo");
            } catch (Exception ex) {
                redirectTarget = isActivation
                        ? frontendVerifyUrl(activationToken, "google_fallo")
                        : frontendLoginUrl(state, "google_fallo");
            }
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(redirectTarget))
                .build();
    }

    /** El frontend canjea aquí el código de un solo uso que recibió en la redirección de
     * /google/callback - esta sí es una llamada normal (fetch, vía el proxy de Vercel), así
     * que aquí sí se pueden fijar las cookies httpOnly igual que en /login. */
    @PostMapping("/google/exchange")
    public ResponseEntity<ApiResponse<AuthResponse>> googleExchange(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.GoogleExchangeRequest request,
            HttpServletResponse httpResponse) {
        AuthResponse response = googleLoginExchangeStore.consume(request.getCode());
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "El enlace de Google expiró o ya fue usado. Intenta de nuevo.", null));
        }
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    /** slug puede venir vacío/null (login global) - en ese caso no hay prefijo de empresa
     * en la URL. */
    private String frontendLoginUrl(String slug, String errorCode) {
        String path = (slug != null && !slug.isBlank()) ? "/" + slug + "/login" : "/login";
        return frontendUrl + path + "?authError="
                + java.net.URLEncoder.encode(errorCode, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** El token va en el fragmento (#) igual que en el enlace original del correo - nunca
     * en la query, para no dejarlo en logs de servidor ni en el Referer. */
    private String frontendVerifyUrl(String token, String errorCode) {
        return frontendUrl + "/auth/verify?authError="
                + java.net.URLEncoder.encode(errorCode, java.nio.charset.StandardCharsets.UTF_8)
                + "#token=" + java.net.URLEncoder.encode(token == null ? "" : token, java.nio.charset.StandardCharsets.UTF_8);
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
    @PreAuthorize("@accesoValidator.can('VISTA_GESTION_CREDENCIALES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> suspendAccount(@PathVariable Integer id) {
        usuarioService.suspendAccount(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta suspendida", null));
    }

    /** Cambio de correo forzado por un admin de la empresa - sin doble confirmación,
     * pensado para cuando la persona perdió el acceso a su correo anterior. */
    @PutMapping("/admin-email-change/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_GESTION_CREDENCIALES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> adminChangeEmail(
            @PathVariable Integer id,
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AdminChangeEmailRequest request) {
        usuarioService.adminChangeEmail(id, request.getNewEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Correo actualizado exitosamente", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        // Por id, no por email: el correo ya no identifica de forma unica a la sesion.
        Integer usuarioId = veterinaria.vargasvet.security.SecurityUtils.getCurrentUserId();
        usuarioService.changePassword(usuarioId, dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada exitosamente", null));
    }

    @PostMapping("/email-change/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.RequestEmailChangeDTO request) {
        Integer usuarioId = veterinaria.vargasvet.security.SecurityUtils.getCurrentUserId();
        emailChangeService.requestChange(usuarioId, request);
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Revisa el correo actual y el nuevo para confirmar el cambio", null));
    }

    @PostMapping("/email-change/confirm-current")
    public ResponseEntity<ApiResponse<Boolean>> confirmCurrentEmail(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ConfirmSecurityTokenDTO request) {
        boolean completed = emailChangeService.confirmCurrentEmail(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(true,
                completed ? "Cambio de correo completado" : "Correo actual confirmado; falta confirmar el nuevo correo",
                completed));
    }

    @PostMapping("/email-change/confirm-new")
    public ResponseEntity<ApiResponse<Boolean>> confirmNewEmail(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ConfirmSecurityTokenDTO request) {
        boolean completed = emailChangeService.confirmNewEmail(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(true,
                completed ? "Cambio de correo completado" : "Correo nuevo confirmado; falta confirmar el correo actual",
                completed));
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
        Integer usuarioId = veterinaria.vargasvet.security.SecurityUtils.getCurrentUserId();
        AuthResponse response = usuarioService.switchRole(usuarioId, request.getRoleId());
        setAuthCookies(httpResponse, response.getToken(), response.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol cambiado exitosamente", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ForgotPasswordRequest request) {
        usuarioService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Si el correo existe, se han enviado las instrucciones para restablecer la contraseña.", null));
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ConfirmSecurityTokenDTO request) {
        boolean isValid = usuarioService.validateResetToken(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Token validado", isValid));
    }

    /** Compatibilidad temporal con el frontend anterior durante un despliegue gradual. */
    @Deprecated(forRemoval = true)
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Boolean>> validateResetTokenLegacy(@RequestParam String token) {
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
