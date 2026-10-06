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
import veterinaria.vargasvet.dto.request.SwitchRoleRequest;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.service.UsuarioService;
import veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final veterinaria.vargasvet.service.EmailChangeService emailChangeService;
    private final GoogleOAuthService googleOAuthService;
    private final GoogleLoginExchangeStore googleLoginExchangeStore;
    private final GoogleOAuthFlowStore googleOAuthFlowStore;
    private final veterinaria.vargasvet.service.AccountClosureService accountClosureService;
    private final veterinaria.vargasvet.security.ClientIpResolver clientIpResolver;

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

    @Value("${app.security.recovery-min-response-ms:400}")
    private long recoveryMinResponseMs;

    @PostConstruct
    void validateCookieConfiguration() {
        if (!java.util.Set.of("Lax", "Strict", "None").contains(cookieSameSite)) {
            throw new IllegalStateException("COOKIE_SAME_SITE debe ser Lax, Strict o None");
        }
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalStateException("Las cookies SameSite=None requieren COOKIE_SECURE=true");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginDTO loginDTO,
                                                           HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.login(loginDTO);
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    @PostMapping("/admin-login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AdminLoginDTO adminLoginDTO,
            HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.adminLogin(adminLoginDTO);
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login exitoso", response));
    }

    private static final String GOOGLE_STATE_COOKIE = "google_oauth_state";
    private static final String GOOGLE_STATE_COOKIE_PATH = "/api/v1/auth/google";

    /** Paso 1: el frontend deja aquí el contexto (clínica o enlace de activación) y recibe un código
     * opaco de un solo uso. Así ni el slug ni el token de invitación viajan por las URL que pasan
     * por Google o por los registros de los servidores. */
    @PostMapping("/google/intent")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> googleIntent(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.GoogleIntentRequest request) {
        boolean hasSlug = request.getSlug() != null && !request.getSlug().isBlank();
        boolean hasToken = request.getActivationToken() != null && !request.getActivationToken().isBlank();
        if (hasSlug == hasToken) {
            throw new IllegalArgumentException("Indica la clínica a la que quieres entrar");
        }
        String intent = googleOAuthFlowStore.createIntent(new GoogleOAuthFlowStore.Context(
                hasSlug ? request.getSlug().trim().toLowerCase(java.util.Locale.ROOT) : null,
                hasToken ? request.getActivationToken().trim() : null));
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud registrada",
                java.util.Map.of("intent", intent)));
    }

    /** Paso 2 (navegación del navegador): fija la cookie que liga este navegador con el flujo y lo
     * envía a Google con un "state" aleatorio y PKCE. Debe atenderse en el mismo dominio que el
     * callback para que la cookie viaje de vuelta. */
    @GetMapping("/google/start")
    public ResponseEntity<Void> googleStart(@RequestParam(required = false) String intent,
                                            HttpServletResponse httpResponse) {
        GoogleOAuthFlowStore.Context context = googleOAuthFlowStore.consumeIntent(intent);
        if (context == null) {
            return redirectTo(frontendLoginUrl(null, "google_fallo"));
        }
        String codeVerifier = veterinaria.vargasvet.security.SecurityTokenUtils.generate();
        String state;
        try {
            state = googleOAuthFlowStore.createAuthorization(
                    new GoogleOAuthFlowStore.Authorization(context, codeVerifier));
        } catch (veterinaria.vargasvet.exception.RateLimitExceededException ex) {
            return redirectTo(frontendLoginUrl(context.slug(), "google_fallo"));
        }
        addGoogleStateCookie(httpResponse, state, 600);
        return redirectTo(googleOAuthService.buildAuthorizationUrl(
                state, GoogleOAuthService.codeChallenge(codeVerifier)));
    }

    /** Adonde Google redirige al navegador tras el consentimiento. No es una llamada del
     * frontend (fetch/XHR) sino una navegación real del navegador, así que nunca devuelve
     * JSON - siempre redirige (éxito o error) de vuelta al frontend. Solo se atiende si el
     * "state" coincide con la cookie fijada por /google/start en este mismo navegador. */
    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @CookieValue(name = GOOGLE_STATE_COOKIE, required = false) String stateCookie,
            HttpServletResponse httpResponse) {
        addGoogleStateCookie(httpResponse, "", 0);

        GoogleOAuthFlowStore.Authorization authorization = matchingAuthorization(state, stateCookie);
        if (authorization == null) {
            return redirectTo(frontendLoginUrl(null, "google_fallo"));
        }
        GoogleOAuthFlowStore.Context context = authorization.context();
        boolean isActivation = context.isActivation();
        String activationToken = context.activationToken();
        String slug = context.slug();

        String redirectTarget;
        if (error != null || code == null || code.isBlank()) {
            redirectTarget = isActivation
                    ? frontendVerifyUrl(activationToken, "google_cancelado")
                    : frontendLoginUrl(slug, "google_cancelado");
        } else {
            try {
                GoogleOAuthService.GoogleIdentity identity =
                        googleOAuthService.resolveIdentity(code, authorization.codeVerifier());
                if (!identity.emailVerified()) {
                    redirectTarget = isActivation
                            ? frontendVerifyUrl(activationToken, "google_email_no_verificado")
                            : frontendLoginUrl(slug, "google_email_no_verificado");
                } else {
                    AuthResponse response = isActivation
                            ? usuarioService.activateAccountWithGoogle(activationToken, identity.email())
                            : usuarioService.loginWithGoogle(identity.email(), slug);
                    String exchangeCode = googleLoginExchangeStore.store(response);
                    redirectTarget = frontendUrl + "/auth/google/callback?code="
                            + java.net.URLEncoder.encode(exchangeCode, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (veterinaria.vargasvet.exception.GoogleEmailMismatchException ex) {
                redirectTarget = isActivation
                        ? frontendVerifyUrl(activationToken, "google_correo_no_coincide")
                        : frontendLoginUrl(slug, "google_fallo");
            } catch (veterinaria.vargasvet.exception.GoogleClinicAccessException ex) {
                redirectTarget = frontendLoginUrl(slug, "google_sin_acceso_clinica");
            } catch (veterinaria.vargasvet.exception.GoogleAccountSuspendedException ex) {
                redirectTarget = frontendLoginUrl(slug, "google_cuenta_suspendida");
            } catch (veterinaria.vargasvet.exception.GoogleAccountClosedException ex) {
                redirectTarget = frontendLoginUrl(slug, "google_cuenta_cerrada");
            } catch (veterinaria.vargasvet.exception.GoogleAccountDeactivatedException ex) {
                redirectTarget = frontendLoginUrl(slug, "google_cuenta_dada_de_baja");
            } catch (org.springframework.security.authentication.DisabledException ex) {
                redirectTarget = isActivation
                        ? frontendVerifyUrl(activationToken, "google_cuenta_no_habilitada")
                        : frontendLoginUrl(slug, "google_cuenta_no_habilitada");
            } catch (Exception ex) {
                redirectTarget = isActivation
                        ? frontendVerifyUrl(activationToken, "google_fallo")
                        : frontendLoginUrl(slug, "google_fallo");
            }
        }
        return redirectTo(redirectTarget);
    }

    private GoogleOAuthFlowStore.Authorization matchingAuthorization(String state, String stateCookie) {
        if (state == null || state.isBlank() || stateCookie == null || stateCookie.isBlank()) {
            return null;
        }
        boolean sameBrowser = java.security.MessageDigest.isEqual(
                state.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                stateCookie.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return sameBrowser ? googleOAuthFlowStore.consumeAuthorization(state) : null;
    }

    private void addGoogleStateCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(GOOGLE_STATE_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(GOOGLE_STATE_COOKIE_PATH)
                .maxAge(java.time.Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseEntity<Void> redirectTo(String target) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(target))
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
        setAuthCookies(httpResponse, response);
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
    public ResponseEntity<ApiResponse<Void>> setupAccount(@Valid @RequestBody veterinaria.vargasvet.dto.request.SetupAccountRequest request,
                                                          jakarta.servlet.http.HttpServletRequest httpRequest) {
        usuarioService.setupAccount(request.getToken(), request.getPassword(), request.getAvisoLeido(),
                clientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Cuenta activada y contraseña creada exitosamente", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email,
                                                                @RequestParam(required = false) String slug) {
        long startedAt = System.nanoTime();
        usuarioService.resendVerificationToken(email, slug);
        padResponseTime(startedAt);
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Si la cuenta requiere verificación, se enviaron las instrucciones.", null));
    }

    @PostMapping("/resend-verification-by-token")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> resendVerificationByToken(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ResendVerificationByTokenRequest request) {
        String maskedEmail = usuarioService.resendVerificationByToken(request.getToken(), request.getSlug());
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Te enviamos un enlace nuevo.", java.util.Map.of("email", maskedEmail)));
    }

    @GetMapping("/profile/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_EMPLEADOS', 'LEER')")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfile(@PathVariable Integer id) {
        UserProfileDTO profile = usuarioService.getProfile(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil obtenido", profile));
    }

    /** Cambio de correo forzado por un admin de la empresa - sin doble confirmación,
     * pensado para cuando la persona perdió el acceso a su correo anterior. */
    @PutMapping("/admin-email-change/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_GESTION_CREDENCIALES', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<Void>> adminChangeEmail(
            @PathVariable Integer id,
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AdminChangeEmailRequest request) {
        var result = emailChangeService.requestAdministrativeChange(id, request.getNewEmail(), request.getMotivo());
        String message = switch (result) {
            case CONFIRMATION_PENDING ->
                    "Enviamos un enlace de confirmación al correo nuevo; el cambio se aplica cuando la persona lo confirme";
            case PENDING_ACCOUNT_CORRECTED_WITH_INVITATION ->
                    "Corregimos el correo y enviamos una invitación nueva de activación al correo corregido";
            case PENDING_ACCOUNT_CORRECTED ->
                    "Corregimos el correo. La invitación se enviará cuando se registre su primera mascota";
        };
        return ResponseEntity.ok(new ApiResponse<>(true, message, null));
    }

    @GetMapping("/account/closure")
    public ResponseEntity<ApiResponse<veterinaria.vargasvet.dto.response.AccountClosureEligibility>> closureEligibility() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Elegibilidad consultada", accountClosureService.eligibility()));
    }

    @PostMapping("/account/closure/request")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> requestAccountClosure(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AccountClosureRequest request) {
        long espera = accountClosureService.requestClosure(request.getPassword());
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Te enviamos un código de 6 dígitos a tu correo. Vale 10 minutos.",
                java.util.Map.of("retryAfterSeconds", espera)));
    }

    @PostMapping("/account/closure/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAccountClosure(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.AccountClosureConfirmRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        accountClosureService.confirmClosure(request.getCode());
        clearAuthCookies(httpResponse, veterinaria.vargasvet.security.SessionCookies.slugOf(httpRequest));
        return ResponseEntity.ok(new ApiResponse<>(true,
                "Cerraste tu cuenta. Te enviamos un correo con el enlace para reactivarla durante los próximos 30 días.", null));
    }

    @PostMapping("/account/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivateAccount(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ReactivateAccountRequest request) {
        accountClosureService.reactivate(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Reactivamos tu cuenta. Ya puedes iniciar sesión.", null));
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

    @PostMapping("/email-change/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelEmailChange(
            @Valid @RequestBody veterinaria.vargasvet.dto.request.ConfirmSecurityTokenDTO request) {
        emailChangeService.cancelRequest(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Solicitud de cambio de correo cancelada", null));
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
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(jakarta.servlet.http.HttpServletRequest request,
                                                             HttpServletResponse httpResponse) {
        AuthResponse response = usuarioService.refreshToken(
                veterinaria.vargasvet.security.SessionCookies.readRefresh(request).orElse(null));
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refrescado exitosamente", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(jakarta.servlet.http.HttpServletRequest request,
                                                      HttpServletResponse httpResponse) {
        usuarioService.revokeRefreshToken(veterinaria.vargasvet.security.SessionCookies.readRefresh(request).orElse(null));
        clearAuthCookies(httpResponse, veterinaria.vargasvet.security.SessionCookies.slugOf(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Sesión cerrada exitosamente", null));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<AuthResponse>> currentSession() {
        Integer usuarioId = veterinaria.vargasvet.security.SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Sesión vigente", usuarioService.currentSession(usuarioId)));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<ApiResponse<AuthResponse>> switchRole(@Valid @RequestBody SwitchRoleRequest request,
                                                                HttpServletResponse httpResponse) {
        Integer usuarioId = veterinaria.vargasvet.security.SecurityUtils.getCurrentUserId();
        AuthResponse response = usuarioService.switchRole(usuarioId, request.getRoleId());
        setAuthCookies(httpResponse, response);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rol cambiado exitosamente", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody veterinaria.vargasvet.dto.request.ForgotPasswordRequest request) {
        long startedAt = System.nanoTime();
        usuarioService.forgotPassword(request);
        padResponseTime(startedAt);
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

    private void setAuthCookies(HttpServletResponse response, AuthResponse auth) {
        String slug = veterinaria.vargasvet.security.SessionCookies.sanitize(auth.getCompanySlug());
        addCookie(response, veterinaria.vargasvet.security.SessionCookies.accessName(slug), auth.getToken(), accessTokenMaxAge, "/api/v1");
        addCookie(response, veterinaria.vargasvet.security.SessionCookies.refreshName(slug), auth.getRefreshToken(), refreshTokenMaxAge, "/api/v1/auth");
    }

    private void clearAuthCookies(HttpServletResponse response, String slug) {
        addCookie(response, veterinaria.vargasvet.security.SessionCookies.accessName(slug), "", 0, "/api/v1");
        addCookie(response, veterinaria.vargasvet.security.SessionCookies.refreshName(slug), "", 0, "/api/v1/auth");
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

    /** Las solicitudes de recuperación responden lo mismo exista o no la cuenta; este piso de
     * tiempo evita que la diferencia entre "no hice nada" y "generé token y envié correo" delate
     * qué correos tienen cuenta. Solo aplica a la respuesta normal (no a los 429). */
    private void padResponseTime(long startedAtNanos) {
        long remainingMs = recoveryMinResponseMs - (System.nanoTime() - startedAtNanos) / 1_000_000L;
        if (remainingMs <= 0) {
            return;
        }
        try {
            Thread.sleep(remainingMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
