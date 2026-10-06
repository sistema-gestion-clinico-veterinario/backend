package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.AssignedRoleResponse;
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.exception.RefreshTokenReuseException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;

import java.util.*;
import java.util.stream.Collectors;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.security.PasswordPolicyService;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.SessionSecurityService;
import veterinaria.vargasvet.service.AuthenticationAuditService;
import veterinaria.vargasvet.domain.enums.RolePurpose;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements veterinaria.vargasvet.service.UsuarioService {

    private static final String NO_ASSIGNED_ROLE_MESSAGE =
            "Todavía no tienes un rol asignado en esta clínica. Pide a tu administrador que te lo asigne para poder ingresar.";
    private static final String DUMMY_BCRYPT_HASH ="$2a$10$7EqJtq98hPqEX7fNZaFWoO5LwR8mH3eQfPJfQZpD1fM9L0f.R8j6u";

    private final UsuarioRepository usuarioRepository;
    private final veterinaria.vargasvet.repository.EmpleadoRepository empleadoRepository;
    private final veterinaria.vargasvet.repository.ApoderadoRepository apoderadoRepository;
    private final veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository credencialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final EmailService emailService;
    private final MenuBuilderService menuBuilderService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogService auditLogService;
    private final CompanyRepository companyRepository;
    private final veterinaria.vargasvet.service.CompanyMembershipService companyMembershipService;
    private final SessionSecurityService sessionSecurityService;
    private final SharedRateLimitService sharedRateLimitService;
    private final veterinaria.vargasvet.security.AccountLockoutService accountLockoutService;
    private final AuthenticationAuditService authenticationAuditService;
    private final PasswordPolicyService passwordPolicyService;
    private final veterinaria.vargasvet.service.LegalDocumentService legalDocumentService;
    private final UsuarioContactoService contactoService;
    private final veterinaria.vargasvet.security.RealtimeSubscriptionGuard realtimeSubscriptionGuard;
    private final veterinaria.vargasvet.service.AdministratorProtection administratorProtection;
    private final veterinaria.vargasvet.service.AccountClosureGuard accountClosureGuard;
    private final veterinaria.vargasvet.service.ConsentimientoDatosService consentimientoDatosService;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.logo}")
    private String companyLogo;

    @Value("${app.company.email}")
    private String companyEmail;

    @Value("${app.company.phone}")
    private String companyPhone;

    @Value("${app.company.address}")
    private String companyAddress;

    @Value("${jwt.absolute-timeout-seconds}")
    private long absoluteTimeoutSeconds;

    @Value("${jwt.refresh-reuse-grace-seconds:0}")
    private long refreshReuseGraceSeconds;

    @Value("${jwt.refresh-validity-in-seconds:604800}")
    private long refreshValiditySeconds;

    @Value("${security.password-reset-validity-minutes:60}")
    private long passwordResetValidityMinutes;

    @Value("${security.verification-token-validity-hours:24}")
    private long verificationTokenValidityHours;

    @Value("${app.rate-limit.login-per-account-per-15-minutes:8}")
    private int loginPerAccountPerWindow;

    @Value("${app.rate-limit.recovery-per-account-per-hour:3}")
    private int recoveryPerAccountPerHour;

    /**
     * Resuelve los datos de marca (nombre, logo, contacto) de la empresa del usuario para
     * personalizar los correos. Si el usuario no tiene empresa asignada, o la empresa no tiene
     * un dato en particular, se usa el valor por defecto de la plataforma como respaldo.
     */
    private Map<String, Object> resolveCompanyBranding(Usuario usuario) {
        return resolveCompanyBranding(usuario, usuario.getCompany());
    }

    private Map<String, Object> resolveCompanyBranding(Usuario usuario, Company company) {
        Map<String, Object> branding = new HashMap<>();
        branding.put("companyName", company != null && company.getName() != null ? company.getName() : companyName);
        branding.put("companyLogo", company != null && company.getLogoUrl() != null ? company.getLogoUrl() : companyLogo);
        branding.put("companyEmail", company != null && company.getEmail() != null ? company.getEmail() : companyEmail);
        branding.put("companyPhone", company != null && company.getPhone() != null ? company.getPhone() : companyPhone);
        branding.put("companyAddress", company != null && company.getAddress() != null ? company.getAddress() : companyAddress);
        return branding;
    }

    private void sendVerificationEmail(Usuario usuario, String verificationToken, Company company) {
        try {
            Map<String, Object> model = new HashMap<>(resolveCompanyBranding(usuario, company));
            model.put("nombre", usuario.getEmail());
            model.put("validityHours", verificationTokenValidityHours);
            String slug = company != null ? company.getSlug() : null;
            model.put("verificationLink", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                    "/auth/verify#token=" + verificationToken, slug));
            model.put("avisoPrivacidadLink", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug("/privacidad", slug));

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido a " + model.get("companyName") + " - Activa tu cuenta",
                    model
            );

            emailService.sendEmailWithRetry(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de verificación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void setupAccount(String token, String password, Boolean avisoLeido, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido o expirado"));
        assertVerificationTokenUsable(usuario);

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId())) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Usa recuperacion de contrasena si necesitas cambiarla.");
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = resolveSingleCredencial(usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo determinar la credencial a configurar para este usuario"));

        Integer companyId = credencial.getCompany() == null ? null : credencial.getCompany().getId();
        if (consentimientoDatosService.hayAvisoPublicado(companyId) && !Boolean.TRUE.equals(avisoLeido)) {
            throw new IllegalArgumentException(
                    "Lee el aviso de privacidad de la clínica y confirma que lo leíste para activar tu cuenta");
        }

        passwordPolicyService.validate(password, usuario.getEmail(), usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(password, credencial.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }
        credencial.setPassword(passwordEncoder.encode(password));
        credencial.setPasswordChanged(true);
        credencialRepository.save(credencial);
        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuario.setVerificationTokenExpiresAt(null);
        usuarioRepository.save(usuario);
        consentimientoDatosService.registrarEnterado(usuario.getId(), companyId,
                veterinaria.vargasvet.domain.enums.CanalConsentimiento.ACTIVACION, null, ipAddress, userAgent);
        authenticationAuditService.record(usuario, "CONFIGURAR_CREDENCIALES",
                "El usuario estableció su contraseña inicial y activó la cuenta.");
    }

    @Override
    @Transactional
    public AuthResponse activateAccountWithGoogle(String token, String googleEmail) {
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido o expirado"));
        assertVerificationTokenUsable(usuario);

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId())) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Inicia sesion o recupera tu contrasena.");
        }

        // El correo de Google debe ser EXACTAMENTE el de la invitacion - sin esto, cualquiera
        // con el enlace (que no revela el correo) podria activar la cuenta de otra persona
        // con su propia cuenta de Google.
        String normalizedGoogleEmail = normalizeSecurityIdentifier(googleEmail);
        if (!normalizedGoogleEmail.equalsIgnoreCase(usuario.getEmail())) {
            throw new veterinaria.vargasvet.exception.GoogleEmailMismatchException();
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = resolveSingleCredencial(usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo determinar la credencial a configurar para este usuario"));

        if (assignedRoleNames(usuario, credencial.getCompany()).isEmpty()) {
            throw new DisabledException(NO_ASSIGNED_ROLE_MESSAGE);
        }

        // Sin contraseña: Google ya verifico la identidad, igual que en cualquier alta por
        // SSO - la persona puede crear una contraseña mas adelante (perfil, o "olvide mi
        // contraseña") si alguna vez la necesita.
        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuario.setVerificationTokenExpiresAt(null);
        usuarioRepository.save(usuario);
        credencial.setActivatedWithGoogle(true);
        credencial.setUltimoAcceso(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);
        authenticationAuditService.record(usuario, "ACTIVAR_CUENTA_GOOGLE",
                "El usuario activó su cuenta con Google, sin crear contraseña.");

        return buildLoginResponse(usuario, credencial.getCompany(), credencial, normalizedGoogleEmail, true);
    }

    @Override
    @Transactional
    public void resendVerificationToken(String email, String slug) {
        email = normalizeSecurityIdentifier(email);
        sharedRateLimitService.enforce("verification-account", email,
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));

        // El correo ya no es unico en toda la plataforma: la empresa la fija el slug del
        // enlace (igual que forgotPassword) y solo cuenta quien tiene una relacion ACTIVA
        // con ella. Asi un correo repetido entre empresas no rompe el reenvio, y quien fue
        // dado de baja antes de activar su cuenta no recibe un enlace nuevo.
        String normalizedSlug = slug == null || slug.isBlank() ? null : slug.trim().toLowerCase(Locale.ROOT);
        Company company = null;
        Usuario usuario = null;
        if (normalizedSlug != null) {
            company = companyRepository.findBySlug(normalizedSlug).orElse(null);
            if (company != null) {
                final Integer companyId = company.getId();
                usuario = usuarioRepository.findAllByEmailIgnoreCase(email).stream()
                        .filter(u -> companyMembershipService.hasActiveMembership(u.getId(), companyId))
                        .findFirst()
                        .orElse(null);
            }
        } else {
            usuario = usuarioRepository.findByEmailAndCompanyIsNull(email)
                    .filter(u -> !companyMembershipService.hasOnlyInactiveMemberships(u.getId()))
                    .orElse(null);
        }

        // Respuesta uniforme: no revelar si la cuenta existe o ya fue activada.
        if (usuario == null) {
            passwordEncoder.matches("verification-probe", DUMMY_BCRYPT_HASH);
            return;
        }

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId()) || usuario.isActivo()) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            return;
        }

        reissueVerificationToken(usuario, company);
    }

    @Override
    @Transactional
    public String resendVerificationByToken(String token, String slug) {
        if (token == null || token.isBlank()) {
            throw new ResourceNotFoundException("El enlace no es válido");
        }
        // El enlace (vencido o no) prueba que la persona recibió la invitación, así que no
        // hace falta que vuelva a escribir su correo: se reenvía a la dirección guardada.
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("El enlace no es válido"));
        sharedRateLimitService.enforce("verification-account", normalizeSecurityIdentifier(usuario.getEmail()),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));

        String normalizedSlug = slug == null || slug.isBlank() ? null : slug.trim().toLowerCase(Locale.ROOT);
        Company company;
        if (normalizedSlug != null) {
            company = companyRepository.findBySlug(normalizedSlug).orElse(null);
            if (company == null || !companyMembershipService.hasActiveMembership(usuario.getId(), company.getId())) {
                throw new ResourceNotFoundException("El enlace no es válido");
            }
        } else {
            company = usuario.getCompany();
            if (companyMembershipService.hasOnlyInactiveMemberships(usuario.getId())) {
                throw new ResourceNotFoundException("El enlace no es válido");
            }
        }

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId()) || usuario.isActivo()) {
            throw new IllegalArgumentException("La cuenta ya fue activada. Inicia sesion o recupera tu contrasena.");
        }

        reissueVerificationToken(usuario, company);
        return maskEmail(usuario.getEmail());
    }

    private void reissueVerificationToken(Usuario usuario, Company company) {
        String newToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(newToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(verificationTokenValidityHours));
        usuarioRepository.save(usuario);

        sendVerificationEmail(usuario, newToken, company);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginDTO loginDTO) {
        String username = normalizeSecurityIdentifier(loginDTO.getUsername());
        loginDTO.setUsername(username);
        String slug = loginDTO.getSlug() == null ? null : loginDTO.getSlug().trim().toLowerCase(Locale.ROOT);
        sharedRateLimitService.enforce("login-account", username,
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));
        accountLockoutService.assertNotLocked(username);

        // Aislamiento total entre empresas: no existe login "global" sin marca de
        // empresa - siempre hace falta el slug de la URL para saber contra cual empresa
        // se valida, porque el username ya no es unico en toda la plataforma (puede
        // repetirse entre empresas distintas sin relacion entre si).
        if (slug == null) {
            authenticationAuditService.recordLoginFailure(null, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        Company company = companyRepository.findBySlug(slug).orElse(null);
        if (company == null) {
            authenticationAuditService.recordLoginFailure(null, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Acepta tanto el username como el correo de contacto en el mismo campo (mas
        // rapido para la persona). Ninguno de los dos es unico en TODA la plataforma
        // (puede repetirse entre empresas sin relacion), asi que se desambigua
        // quedandose con el candidato que tenga membresia activa en ESTA empresa - la
        // misma verificacion que antes, solo que ahora tambien filtra duplicados de
        // otras empresas en vez de asumir un unico candidato global.
        Usuario usuario = usuarioRepository.findAllByUsernameIgnoreCase(username).stream()
                .filter(u -> companyMembershipService.hasActiveMembership(u.getId(), company.getId()))
                .findFirst()
                .or(() -> usuarioRepository.findAllByEmailIgnoreCase(username).stream()
                        .filter(u -> companyMembershipService.hasActiveMembership(u.getId(), company.getId()))
                        .findFirst())
                .orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(loginDTO.getPassword(), DUMMY_BCRYPT_HASH);
            authenticationAuditService.recordLoginFailure(null, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), company.getId()).orElse(null);
        if (credencial == null || !passwordEncoder.matches(loginDTO.getPassword(), credencial.getPassword())) {
            authenticationAuditService.recordLoginFailure(usuario, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (passwordEncoder.upgradeEncoding(credencial.getPassword())) {
            credencial.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
        }
        credencial.setUltimoAcceso(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);
        accountLockoutService.registerSuccessfulLogin(username);

        return buildLoginResponse(usuario, company, credencial, username, false);
    }

    /** Login vía "Continuar con Google". Google ya verificó que la persona controla ese
     * correo (reemplaza a la contraseña), así que este método omite por completo el
     * chequeo de credencial.password - pero NO crea cuentas ni credenciales nuevas: si
     * no existe ya un Usuario con ese email y una UsuarioEmpresaCredencial para la
     * empresa resuelta, se rechaza igual que un login con contraseña incorrecta (mismo
     * mensaje genérico, para no revelar si el correo existe). */
    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String email, String slug) {
        String normalizedEmail = normalizeSecurityIdentifier(email);
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (normalizedSlug.isEmpty()) {
            authenticationAuditService.recordLoginFailure(null, normalizedEmail, "inicio de sesión de Google sin clínica");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        sharedRateLimitService.enforce("login-account", normalizedEmail,
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));
        accountLockoutService.assertNotLocked(normalizedEmail);

        // El correo puede existir en varias empresas. Google acredita el correo, pero el
        // slug decide qué cuenta concreta se está autenticando; jamás se toma la primera
        // coincidencia global porque revelaría o adoptaría la identidad de otra clínica.
        List<Usuario> emailCandidates = usuarioRepository.findAllByEmailIgnoreCase(normalizedEmail);
        if (emailCandidates.isEmpty()) {
            authenticationAuditService.recordLoginFailure(null, normalizedEmail, "correo de Google sin cuenta");
            throw new veterinaria.vargasvet.exception.GoogleClinicAccessException();
        }
        Company company = companyRepository.findBySlug(normalizedSlug).orElse(null);
        if (company == null) {
            authenticationAuditService.recordLoginFailure(null, normalizedEmail, "empresa de Google inexistente");
            throw new veterinaria.vargasvet.exception.GoogleClinicAccessException();
        }
        Usuario usuario = emailCandidates.stream()
                .filter(u -> companyMembershipService.hasAnyMembership(u.getId(), company.getId()))
                .findFirst()
                .orElse(null);
        if (usuario == null) {
            authenticationAuditService.recordLoginFailure(emailCandidates.get(0), normalizedEmail,
                    "cuenta de Google sin acceso a la empresa");
            throw new veterinaria.vargasvet.exception.GoogleClinicAccessException();
        }
        if (!companyMembershipService.hasActiveMembership(usuario.getId(), company.getId())) {
            if (accountClosureGuard.hasOpenClosure(usuario.getId(), company.getId())) {
                authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "cuenta cerrada por la persona (con Google)");
                throw new veterinaria.vargasvet.exception.GoogleAccountClosedException();
            }
            if (companyMembershipService.isSuspendedIn(usuario.getId(), company.getId())) {
                authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "cuenta suspendida (con Google)");
                throw new veterinaria.vargasvet.exception.GoogleAccountSuspendedException();
            }
            if (companyMembershipService.isDeactivatedIn(usuario.getId(), company.getId())) {
                authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "cuenta dada de baja (con Google)");
                throw new veterinaria.vargasvet.exception.GoogleAccountDeactivatedException();
            }
            authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "cuenta de Google sin acceso a la empresa");
            throw new veterinaria.vargasvet.exception.GoogleClinicAccessException();
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), company.getId()).orElse(null);
        if (credencial == null) {
            authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "cuenta de Google sin credencial en la empresa");
            throw new veterinaria.vargasvet.exception.GoogleClinicAccessException();
        }

        credencial.setUltimoAcceso(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);
        accountLockoutService.registerSuccessfulLogin(normalizedEmail);

        return buildLoginResponse(usuario, company, credencial, normalizedEmail, true);
    }

    /** Quien activó su cuenta con Google no tiene una contraseña que cambiar: no se le pide una. */
    private boolean isPasswordPromptSatisfied(veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial) {
        return credencial.isPasswordChanged() || credencial.isActivatedWithGoogle();
    }

    private List<String> assignedRoleNames(Usuario usuario, Company company) {
        return usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getCompany() != null && upr.getCompany().getId().equals(company.getId()))
                .filter(upr -> isRoleOperable(usuario.getId(), upr))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());
    }

    /** Cola compartida por login() (con contraseña) y loginWithGoogle() (sin contraseña):
     * una vez identificada la persona, la empresa y su credencial en esa empresa, el resto
     * del proceso (estado de cuenta, roles activos, emisión de JWT, auditoría) es idéntico. */
    private AuthResponse buildLoginResponse(Usuario usuario, Company company,
            veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial, String username,
            boolean viaGoogle) {
        String via = viaGoogle ? " (con Google)" : "";
        if (!usuario.isEmailVerified()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada" + via);
            throw new DisabledException("Tu cuenta aún no ha sido verificada. Por favor, revisa tu correo electrónico.");
        }

        if (!usuario.isActivo()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada" + via);
            throw new DisabledException("La cuenta está suspendida");
        }

        if (!company.isActivo()) {
            throw new DisabledException("Acceso denegado. La empresa está inactiva. Contacta al administrador.");
        }

        // Solo los roles de ESTA empresa (la del slug) - si la persona
        // tambien trabaja o es cliente en otra empresa, esos roles no deben
        // verse ni activarse aqui.
        List<String> assignedRoles = assignedRoleNames(usuario, company);
        if (assignedRoles.isEmpty()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "sin rol asignado" + via);
            throw new DisabledException(NO_ASSIGNED_ROLE_MESSAGE);
        }

        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, null, null, company.getId());
        if (activeAssignment == null) {
            throw new DisabledException("Tu rol activo se encuentra desactivado. Contacta al administrador.");
        }
        String activeRole = activeAssignment.getRol().getName();

        List<String> activeRolesList = java.util.Collections.singletonList(activeRole);

        Integer companyId = company.getId();

        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String sessionId = UUID.randomUUID().toString();
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, companyId,
                credencial.getCredentialsVersion(), sessionId);
        String refreshToken = createRefreshToken(usuario, activeAssignment, Instant.now(), sessionId,
                company, credencial.getCredentialsVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, companyId));
        response.setCompanyId(companyId);
        response.setCompanyName(company.getName());
        response.setCompanyLogoUrl(company.getLogoUrl());
        response.setCompanyColorPrimario(company.getColorPrimario());
        response.setCompanySlug(company.getSlug());
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario, companyId));
        response.setPasswordChanged(isPasswordPromptSatisfied(credencial));
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setEmpleadoId(resolveActiveEmpleadoId(usuario, companyId));
        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        // Registrar log de auditoría para Login
        auditLogService.log(
            usuario.getEmail(),
            activeRole,
            companyId,
            company.getName(),
            "LOGIN_EXITOSO",
            "Seguridad",
            "Inicio de sesión exitoso del usuario " + usuario.getUsername() + " con rol activo " + activeRole
                    + (viaGoogle ? " mediante Google" : " mediante contraseña"),
            null
        );

        return response;
    }

    @Override
    @Transactional
    public AuthResponse adminLogin(veterinaria.vargasvet.dto.request.AdminLoginDTO adminLoginDTO) {
        String username = normalizeSecurityIdentifier(adminLoginDTO.getUsername());
        sharedRateLimitService.enforce("login-account", username,
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));
        accountLockoutService.assertNotLocked(username);

        // SuperAdmin siempre tiene company = null - el namespace de username "sin
        // empresa" no se toca con el aislamiento entre empresas (esas cuentas nunca
        // pertenecieron a una empresa en particular).
        Usuario usuario = usuarioRepository.findByUsernameAndCompanyIsNull(username).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(adminLoginDTO.getPassword(), DUMMY_BCRYPT_HASH);
            authenticationAuditService.recordLoginFailure(null, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), null).orElse(null);
        if (credencial == null || !passwordEncoder.matches(adminLoginDTO.getPassword(), credencial.getPassword())) {
            authenticationAuditService.recordLoginFailure(usuario, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);
        if (!esSuperAdmin) {
            // Mismo mensaje generico: no revelar que las credenciales eran correctas
            // pero la cuenta no es de SuperAdmin.
            authenticationAuditService.recordLoginFailure(usuario, username, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (passwordEncoder.upgradeEncoding(credencial.getPassword())) {
            credencial.setPassword(passwordEncoder.encode(adminLoginDTO.getPassword()));
        }
        credencial.setUltimoAcceso(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);
        accountLockoutService.registerSuccessfulLogin(username);

        if (!usuario.isEmailVerified()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada");
            throw new DisabledException("Tu cuenta aún no ha sido verificada. Por favor, revisa tu correo electrónico.");
        }

        if (!usuario.isActivo()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada");
            throw new DisabledException("La cuenta está suspendida");
        }

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());
        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, null, null, null);
        if (activeAssignment == null) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada");
            throw new DisabledException("Tu rol activo se encuentra desactivado. Contacta al administrador.");
        }
        String activeRole = activeAssignment.getRol().getName();
        List<String> activeRolesList = java.util.Collections.singletonList(activeRole);

        // SuperAdmin no pertenece a una empresa en particular - companyId nulo,
        // el mismo "modo global" que ya usa DashboardServiceImpl.
        Integer activeRoleId = activeAssignment.getRol().getId();
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String sessionId = UUID.randomUUID().toString();
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, null,
                credencial.getCredentialsVersion(), sessionId);
        String refreshToken = createRefreshToken(usuario, activeAssignment, Instant.now(), sessionId,
                null, credencial.getCredentialsVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, null));
        response.setCompanyId(null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario, null));
        response.setPasswordChanged(credencial.isPasswordChanged());
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        auditLogService.log(
            usuario.getUsername(),
            activeRole,
            null,
            null,
            "LOGIN_EXITOSO",
            "Seguridad",
            "Inicio de sesión de SuperAdmin " + usuario.getUsername(),
            null
        );

        return response;
    }

    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse switchRole(Integer usuarioId, Integer roleId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // La empresa de la sesion ACTUAL (del JWT), no usuario.company (ambiguo
        // si la persona tiene relaciones activas en mas de una empresa) - solo
        // se puede cambiar de rol dentro de la misma empresa con la que se
        // inicio sesion, nunca "saltar" a un rol de otra empresa.
        Integer companyId = SecurityUtils.getCurrentCompanyId();

        List<UsuarioPorRol> companyAssignments = usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        ? upr.getCompany() == null
                        : upr.getCompany() != null && companyId.equals(upr.getCompany().getId()))
                .toList();
        List<String> assignedRoles = companyAssignments.stream()
                .filter(upr -> companyId == null || isRoleOperable(usuario.getId(), upr))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = requireSwitchableAssignment(companyAssignments, usuario.getId(), roleId);
        RefreshToken currentSession = requireCurrentSession(usuario);

        String roleName = activeAssignment.getRol().getName();
        String previousRoles = String.join(", ", SecurityUtils.getCurrentRoleNames());

        List<String> activeRolesList = java.util.Collections.singletonList(roleName);
        Company company = companyId != null ? companyRepository.findById(companyId).orElse(null) : null;

        UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), companyId).orElseThrow(
                        () -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        Integer activeRoleId = activeAssignment.getRol().getId();
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        // Se reemplaza solo ESTA sesión: las de otros dispositivos o empresas siguen abiertas.
        // La nueva hereda el inicio de la actual, así que cambiar de rol no renueva el límite absoluto.
        String previousSessionId = currentSession.getFamilyId();
        revokeFamily(previousSessionId, veterinaria.vargasvet.util.AppClock.instantNow());
        realtimeSubscriptionGuard.closeConnectionsOfSession(previousSessionId);
        String sessionId = UUID.randomUUID().toString();
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, companyId,
                credencial.getCredentialsVersion(), sessionId);
        String refreshToken = createRefreshToken(usuario, activeAssignment, currentSession.getSessionStartedAt(),
                sessionId, company, credencial.getCredentialsVersion());

        AuthResponse response = buildSessionResponse(usuario, activeAssignment, companyId, company, credencial,
                assignedRoles, activeRolesList, menu, permissions);
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);

        // Registrar log de auditoría para cambio de rol
        auditLogService.log(
            usuario.getEmail(),
            roleName,
            companyId,
            company != null ? company.getName() : null,
            "CAMBIO_ROL",
            "Seguridad",
            "Cambio de rol activo del usuario de " + (previousRoles.isBlank() ? "desconocido" : previousRoles)
                    + " a " + roleName,
            null
        );

        return response;
    }

    /** Un rol que no es de la cuenta se rechaza (403); no se sustituye en silencio por otro. */
    private UsuarioPorRol requireSwitchableAssignment(List<UsuarioPorRol> companyAssignments, Integer userId,
                                                      Integer roleId) {
        List<UsuarioPorRol> sameRole = companyAssignments.stream()
                .filter(upr -> Objects.equals(upr.getRol().getId(), roleId))
                .toList();
        if (sameRole.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "El rol seleccionado no pertenece a tu cuenta");
        }
        return sameRole.stream()
                .filter(upr -> upr.getRol().isActivo())
                .filter(upr -> isRoleOperable(userId, upr))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado se encuentra desactivado"));
    }

    /** La sesión que se está usando, identificada por el `sid` del access token y bloqueada para reemplazarla. */
    private RefreshToken requireCurrentSession(Usuario usuario) {
        String sessionId = SecurityUtils.getCurrentSessionId();
        if (sessionId == null) {
            // Access token anterior a los identificadores de sesión: el cliente lo renueva y reintenta.
            throw new BadCredentialsException("La sesión debe renovarse. Inténtalo nuevamente.");
        }
        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        RefreshToken current = refreshTokenRepository.findActiveByFamilyIdForUpdate(sessionId).stream()
                .filter(token -> Objects.equals(token.getUsuario().getId(), usuario.getId()))
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("La sesión ya no está activa. Inicie sesión nuevamente."));
        if (current.getExpiryDate().isBefore(now)
                || current.getSessionStartedAt().plusSeconds(absoluteTimeoutSeconds).isBefore(now)) {
            revokeFamily(sessionId, now);
            throw new BadCredentialsException("La sesión ha expirado. Por favor, inicie sesión nuevamente.");
        }
        return current;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse currentSession(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Company company = companyId != null ? companyRepository.findById(companyId).orElse(null) : null;
        UsuarioEmpresaCredencial credencial = resolveCredencial(usuario.getId(), companyId).orElseThrow(
                () -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        List<UsuarioPorRol> companyAssignments = usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        ? upr.getCompany() == null
                        : upr.getCompany() != null && companyId.equals(upr.getCompany().getId()))
                .toList();
        List<String> assignedRoles = companyAssignments.stream()
                .filter(upr -> companyId == null || isRoleOperable(usuario.getId(), upr))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());
        Integer tokenRoleId = SecurityUtils.getCurrentRoleId();
        UsuarioPorRol activeAssignment = null;
        if (tokenRoleId != null) {
            try {
                activeAssignment = requireSwitchableAssignment(companyAssignments, usuario.getId(), tokenRoleId);
            } catch (org.springframework.security.access.AccessDeniedException | IllegalArgumentException ex) {
                throw new DisabledException("El rol de la sesión ya no está disponible");
            }
        }
        List<String> activeRoles = activeAssignment != null
                ? Collections.singletonList(activeAssignment.getRol().getName())
                : Collections.emptyList();
        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);

        return buildSessionResponse(usuario, activeAssignment, companyId, company, credencial,
                assignedRoles, activeRoles, menu, permissions);
    }

    @Override
    public UserProfileDTO getProfile(Integer id) {
        Usuario usuario = findManageableUser(id);
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        UserProfileDTO profile = userMapper.toProfileDTO(usuario);
        profile.setTelefono(contactoService.telefono(usuario.getId(), companyId));
        profile.setDireccion(contactoService.direccion(usuario.getId(), companyId));
        return profile;
    }

    @Override
    @Transactional
    public void changePassword(Integer usuarioId, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Cambia solo la credencial de la empresa con la que se inició la sesión actual -
        // nunca la de otra empresa donde la persona también tenga cuenta.
        Integer companyId = SecurityUtils.getCurrentCompanyId();

        // Comprobar la contraseña actual es lo que impide que una sesión robada cambie la
        // credencial: sin tope, esa comprobación se podría adivinar por fuerza bruta.
        sharedRateLimitService.enforce("change-password-account",
                usuarioId + ":" + (companyId == null ? "global" : companyId),
                5, java.time.Duration.ofMinutes(15));

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuarioId, companyId)
                        .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        if (!passwordEncoder.matches(dto.getOldPassword(), credencial.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        passwordPolicyService.validate(dto.getNewPassword(), usuario.getEmail(),
                usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(dto.getNewPassword(), credencial.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }

        credencial.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        credencial.setPasswordChanged(true);
        credencialRepository.save(credencial);
        sessionSecurityService.invalidateSessionsForCredential(credencial);

        // Registrar log de auditoría para cambio de contraseña propio
        auditLogService.log(
            "CAMBIO_CONTRASENA",
            "Seguridad",
            "El usuario cambió su contraseña"
        );

        // Enviar notificación informativa
        sendPasswordChangeNotification(usuario);
    }

    @Override
    @Transactional
    public void requestPasswordReset(veterinaria.vargasvet.dto.request.AdminPasswordResetRequest dto) {
        boolean platformAdmin = SecurityUtils.isSuperAdmin();
        Usuario usuario = findPasswordResetTarget(dto, platformAdmin);

        if (!usuario.isActivo() || !usuario.isEmailVerified()) {
            throw new IllegalStateException(
                    "La cuenta todavía no está habilitada; debe completar primero su activación");
        }

        Integer companyId = platformAdmin ? dto.getCompanyId() : SecurityUtils.getCurrentCompanyId();
        Company company = null;
        if (companyId != null) {
            company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
            administratorProtection.assertCanManage(usuario, companyId);
            if (!companyMembershipService.hasActiveMembership(usuario.getId(), companyId)) {
                throw new IllegalStateException("La persona no tiene acceso activo en esta empresa");
            }
        }

        sharedRateLimitService.enforce("admin-reset-account",
                (companyId == null ? "global" : companyId) + ":" + normalizeSecurityIdentifier(usuario.getEmail()),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));

        resolveCredencial(usuario.getId(), companyId)
                .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        String token = createPasswordResetToken(usuario, company);
        auditLogService.log(companyId, "SOLICITAR_RESET_ADMINISTRATIVO", "Seguridad",
                "Solicitó el restablecimiento de acceso de " + resolveNombreCompleto(usuario)
                        + " (" + usuario.getEmail() + ")");
        sendPasswordResetEmail(usuario, company, token, true);
    }

    private Usuario findPasswordResetTarget(veterinaria.vargasvet.dto.request.AdminPasswordResetRequest dto,
                                            boolean platformAdmin) {
        if (dto.getUserId() != null) {
            return findManageableUser(dto.getUserId());
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar el ID de usuario o el correo electrónico");
        }
        String email = normalizeSecurityIdentifier(dto.getEmail());
        if (platformAdmin && dto.getCompanyId() != null) {
            return findByEmailInCompany(email, dto.getCompanyId());
        }
        return findManageableUser(email);
    }

    private void sendPasswordChangeNotification(Usuario usuario) {
        try {
            Map<String, Object> model = new HashMap<>(resolveCompanyBranding(usuario));
            model.put("nombre", resolveNombreCompleto(usuario));
            model.put("email", usuario.getEmail());
            model.put("appUrl", appUrl);

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Notificación de Cambio de Contraseña - " + model.get("companyName"),
                    model
            );

            emailService.sendEmail(mail, "email/password-change-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de notificación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void forgotPassword(veterinaria.vargasvet.dto.request.ForgotPasswordRequest request) {
        request.setEmail(normalizeSecurityIdentifier(request.getEmail()));
        sharedRateLimitService.enforce("password-reset-account", request.getEmail(),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));

        // La empresa la resuelve el slug de la pantalla desde la que se pide el reset
        // (igual que en login) - el correo ya no es unico en toda la plataforma (puede
        // repetirse entre empresas sin relacion), asi que sin slug es imposible saber a
        // cual cuenta restablecer: se desambigua igual que el login, quedandose con el
        // candidato que tenga membresia activa en la empresa del slug.
        String slug = request.getSlug() == null ? null : request.getSlug().trim().toLowerCase(Locale.ROOT);
        Company company = null;
        Usuario usuario;
        if (slug != null) {
            company = companyRepository.findBySlug(slug).orElse(null);
            if (company == null) {
                return;
            }
            final Integer companyId = company.getId();
            usuario = usuarioRepository.findAllByEmailIgnoreCase(request.getEmail()).stream()
                    .filter(u -> companyMembershipService.hasActiveMembership(u.getId(), companyId))
                    .findFirst()
                    .orElse(null);
        } else {
            // Sin slug (pantalla global/SuperAdmin): solo apunta a la credencial sin
            // empresa, unica por correo dentro de ese grupo (indice unico parcial).
            usuario = usuarioRepository.findByEmailAndCompanyIsNull(request.getEmail()).orElse(null);
        }

        if (usuario == null || !usuario.isActivo() || !usuario.isEmailVerified()) {
            return;
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), company != null ? company.getId() : null).orElse(null);
        if (credencial == null) {
            return;
        }

        issuePasswordResetToken(usuario, company, credencial, "SOLICITAR_RESTABLECER_PASSWORD",
                "El usuario solicitó restablecer su contraseña");
    }

    private void issuePasswordResetToken(Usuario usuario, Company company,
                                         veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial,
                                         String action, String detail) {
        String token = createPasswordResetToken(usuario, company);

        try {
            sendPasswordResetEmail(usuario, company, token, false);

            auditLogService.log(
                usuario.getEmail(),
                "USER",
                company != null ? company.getId() : null,
                company != null ? company.getName() : companyName,
                action,
                "Seguridad",
                detail,
                null
            );
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de recuperación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    private String createPasswordResetToken(Usuario usuario, Company company) {
        if (company != null) {
            passwordResetTokenRepository.deleteByUsuarioAndCompany(usuario, company);
        } else {
            passwordResetTokenRepository.deleteByUsuarioAndCompanyIsNull(usuario);
        }
        // Flush el delete ahora: si no, Hibernate ordena el INSERT del nuevo token antes
        // del DELETE y viola el indice unico cuando ya habia un token pendiente.
        passwordResetTokenRepository.flush();

        String token = SecurityTokenUtils.generate();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashToken(token))
                .usuario(usuario)
                .company(company)
                .expiryDate(veterinaria.vargasvet.util.AppClock.now().plusMinutes(passwordResetValidityMinutes))
                .build();

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    private void sendPasswordResetEmail(Usuario usuario, Company company, String token, boolean initiatedByAdmin) {
        Map<String, Object> model = new HashMap<>(resolveCompanyBranding(usuario, company));
        model.put("usuario", resolveNombreCompleto(usuario));
        model.put("initiatedByAdmin", initiatedByAdmin);

        String slug = company != null ? company.getSlug() : null;
        String resetUrl = appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                "/reset-password#token=" + token, slug);
        model.put("resetUrl", resetUrl);

        Mail mail = emailService.createMail(
                usuario.getEmail(),
                "Restablecer Contraseña - " + model.get("companyName"),
                model
        );
        emailService.sendEmailWithRetry(mail, "email/forgot-password-template");
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenForUpdate(hashToken(request.getToken()))
                .orElseThrow(() -> new ResourceNotFoundException("El token es inválido o no existe."));

        // 404 (no 400) para que la pantalla ofrezca pedir un enlace nuevo en vez de un error de formulario.
        if (resetToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.now())) {
            throw new ResourceNotFoundException("El token ha expirado. Por favor solicite uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        Company company = resetToken.getCompany();
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), company != null ? company.getId() : null)
                        .orElseThrow(() -> new IllegalStateException("No se encontró la credencial a restablecer"));

        passwordPolicyService.validate(request.getNewPassword(), usuario.getEmail(),
                usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(request.getNewPassword(), credencial.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }
        credencial.setPassword(passwordEncoder.encode(request.getNewPassword()));
        credencial.setPasswordChanged(true);
        credencialRepository.save(credencial);
        sessionSecurityService.invalidateSessionsForCredential(credencial);

        // El login acepta usuario o correo y cada uno lleva su propio contador de intentos.
        accountLockoutService.clearLockout(usuario.getUsername());
        accountLockoutService.clearLockout(usuario.getEmail());

        passwordResetTokenRepository.delete(resetToken);

        auditLogService.log(
            usuario.getEmail(),
            "USER",
            company != null ? company.getId() : null,
            company != null ? company.getName() : companyName,
            "RESTABLECER_PASSWORD",
            "Seguridad",
            "El usuario ha restablecido su contraseña exitosamente usando un token.",
            null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return passwordResetTokenRepository.findByToken(hashToken(token))
                .map(resetToken -> !resetToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.now()))
                .orElse(false);
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, DisabledException.class})
    public AuthResponse refreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token inválido o expirado. Por favor, inicie sesión nuevamente.");
        }

        TokenProvider.RefreshTokenDetails tokenDetails;
        try {
            tokenDetails = tokenProvider.getRefreshTokenDetails(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Refresh token inválido o expirado. Por favor, inicie sesión nuevamente.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(hashToken(token))
                .orElseThrow(() -> new BadCredentialsException(
                        "Refresh token inválido o expirado. Por favor, inicie sesión nuevamente."));

        boolean tokenMismatch = !Objects.equals(refreshToken.getJti(), tokenDetails.jti())
                || !Objects.equals(refreshToken.getFamilyId(), tokenDetails.familyId())
                || !Objects.equals(refreshToken.getUsuario().getEmail(), tokenDetails.email());
        boolean rotadoHaceUnMomento = !tokenMismatch
                && refreshReuseGraceSeconds > 0
                && refreshToken.getUsedAt() != null
                && refreshToken.getUsedAt().equals(refreshToken.getRevokedAt())
                && refreshToken.getUsedAt().plusSeconds(refreshReuseGraceSeconds)
                        .isAfter(veterinaria.vargasvet.util.AppClock.instantNow())
                && refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull(refreshToken.getFamilyId());
        if (tokenMismatch
                || ((refreshToken.getUsedAt() != null || refreshToken.getRevokedAt() != null) && !rotadoHaceUnMomento)) {
            revokeFamily(refreshToken.getFamilyId(), veterinaria.vargasvet.util.AppClock.instantNow());
            auditLogService.log(refreshToken.getUsuario().getEmail(), "USER",
                    refreshToken.getUsuario().getCompany() != null ? refreshToken.getUsuario().getCompany().getId() : null,
                    refreshToken.getUsuario().getCompany() != null ? refreshToken.getUsuario().getCompany().getName() : null,
                    "REFRESH_TOKEN_REUTILIZADO", "Seguridad",
                    "Se revocó una familia de sesión por reutilización de refresh token.", null);
            throw new RefreshTokenReuseException();
        }

        if (refreshToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.instantNow())) {
            refreshToken.setRevokedAt(veterinaria.vargasvet.util.AppClock.instantNow());
            refreshTokenRepository.save(refreshToken);
            throw new BadCredentialsException("Refresh token expirado. Por favor, inicie sesión nuevamente.");
        }

        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        if (refreshToken.getSessionStartedAt().plusSeconds(absoluteTimeoutSeconds).isBefore(now)) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new BadCredentialsException("La sesión ha expirado. Por favor, inicie sesión nuevamente.");
        }

        Usuario usuario = refreshToken.getUsuario();

        // La empresa CON LA QUE SE ABRIO esta sesion en particular (persistida
        // en el propio refresh token), no usuario.company - una persona puede
        // tener relaciones activas en mas de una empresa a la vez, y esa cache
        // legacy queda en null en ese caso (ambigua). Restaurar siempre la
        // MISMA empresa de la sesion evita que un refresh "salte" al rol de
        // otra empresa distinta a la que se inicio sesion. También determina qué
        // credencial (y por tanto qué credentialsVersion) hay que validar.
        Company sessionCompany = refreshToken.getCompany();
        Integer companyId = sessionCompany != null ? sessionCompany.getId() : null;

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), companyId).orElse(null);
        if (credencial == null || tokenDetails.credentialsVersion() != credencial.getCredentialsVersion()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new BadCredentialsException(
                    "La sesión fue invalidada por un evento de seguridad. Inicie sesión nuevamente.");
        }

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);

        if (!usuario.isActivo()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new DisabledException("La cuenta está suspendida");
        }

        if (!esSuperAdmin && sessionCompany != null && !sessionCompany.isActivo()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new DisabledException("La empresa está desactivada. Contacta al administrador del sistema.");
        }

        List<String> userRoles = usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        || (upr.getCompany() != null && companyId.equals(upr.getCompany().getId())))
                .filter(upr -> companyId == null || isRoleOperable(usuario.getId(), upr))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = resolveActiveAssignment(
                usuario, tokenDetails.activeRoleId(), tokenDetails.activeRole(), companyId);
        if (activeAssignment == null && !userRoles.isEmpty()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new DisabledException("El rol de la sesión ya no está disponible");
        }
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;
        List<String> activeRolesList = activeRole != null
                ? Collections.singletonList(activeRole)
                : Collections.emptyList();

        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String newJwt = createAccessToken(usuario, activeAssignment, activeRolesList, companyId,
                credencial.getCredentialsVersion(), refreshToken.getFamilyId());

        if (!rotadoHaceUnMomento) {
            refreshToken.setUsedAt(now);
            refreshToken.setRevokedAt(now);
            refreshTokenRepository.save(refreshToken);
        }
        String newRefreshToken = createRefreshToken(usuario, activeAssignment,
                refreshToken.getSessionStartedAt(), refreshToken.getFamilyId(), sessionCompany,
                credencial.getCredentialsVersion());

        AuthResponse response = buildSessionResponse(usuario, activeAssignment, companyId, sessionCompany,
                credencial, userRoles, activeRolesList, menu, permissions);
        response.setToken(newJwt);
        response.setRefreshToken(newRefreshToken);

        return response;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hashToken(refreshToken))
                .ifPresent(token -> {
                    revokeFamily(token.getFamilyId(), veterinaria.vargasvet.util.AppClock.instantNow());
                    realtimeSubscriptionGuard.closeConnectionsOfSession(token.getFamilyId());
                    Usuario usuario = token.getUsuario();
                    auditLogService.log(
                            usuario.getEmail(), null,
                            usuario.getCompany() != null ? usuario.getCompany().getId() : null,
                            usuario.getCompany() != null ? usuario.getCompany().getName() : null,
                            "LOGOUT", "Seguridad",
                            "Cierre de sesión del usuario " + usuario.getEmail(), null);
                });
    }

    private String createRefreshToken(Usuario usuario, UsuarioPorRol activeAssignment, Instant sessionStartedAt,
                                      String familyId, Company company, long credentialsVersion) {
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;
        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        String token = tokenProvider.createRefreshToken(usuario.getEmail(), activeRole, activeRoleId,
                familyId, credentialsVersion);
        TokenProvider.RefreshTokenDetails details = tokenProvider.getRefreshTokenDetails(token);
        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        Instant expiryDate = now.plusSeconds(refreshValiditySeconds);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .company(company)
                .tokenHash(hashToken(token))
                .jti(details.jti())
                .familyId(familyId)
                .expiryDate(expiryDate)
                .sessionStartedAt(sessionStartedAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String hashToken(String token) {
        return SecurityTokenUtils.hash(token);
    }

    private String normalizeSecurityIdentifier(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void assertVerificationTokenUsable(Usuario usuario) {
        if (usuario.getVerificationTokenExpiresAt() == null
                || usuario.getVerificationTokenExpiresAt().isBefore(veterinaria.vargasvet.util.AppClock.now())) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new ResourceNotFoundException("El enlace de activación es inválido o expiró");
        }
        // Un enlace emitido antes de la baja no debe permitir que la persona se reactive sola.
        if (companyMembershipService.hasOnlyInactiveMemberships(usuario.getId())) {
            throw new ResourceNotFoundException("Token de verificacion invalido o expirado");
        }
    }

    private void revokeFamily(String familyId, Instant revokedAt) {
        if (familyId == null) return;
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByFamilyIdAndRevokedAtIsNull(familyId);
        activeTokens.forEach(token -> token.setRevokedAt(revokedAt));
        refreshTokenRepository.saveAll(activeTokens);
    }

    private Usuario findManageableUser(Integer userId) {
        if (SecurityUtils.isSuperAdmin()) {
            return usuarioRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        }
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return usuarioRepository.findById(userId)
                .filter(usuario -> companyMembershipService.hasAnyMembership(usuario.getId(), companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Usuario findManageableUser(String email) {
        if (SecurityUtils.isSuperAdmin()) {
            return usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        }
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return findByEmailInCompany(email, companyId);
    }

    private Usuario findByEmailInCompany(String email, Integer companyId) {
        return usuarioRepository.findAllByEmailIgnoreCase(email).stream()
                .filter(usuario -> companyMembershipService.hasAnyMembership(usuario.getId(), companyId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /** companyId nulo = sin filtrar por empresa (SuperAdmin, cuyos roles no
     * pertenecen a ninguna empresa). Con companyId, solo se consideran las
     * asignaciones de rol de ESA empresa - una persona con relaciones en
     * varias empresas (ej. empleado en A, cliente en B) solo debe ver/activar
     * los roles de la empresa en la que inicio sesion, nunca los de otra. */
    private UsuarioPorRol resolveActiveAssignment(Usuario usuario, Integer preferredRoleId, String preferredRoleName,
                                                  Integer companyId) {
        List<UsuarioPorRol> activeAssignments = usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getRol().isActivo())
                .filter(upr -> companyId == null
                        || (upr.getCompany() != null && companyId.equals(upr.getCompany().getId())))
                .filter(upr -> isRoleOperable(usuario.getId(), upr))
                .toList();

        if (preferredRoleId != null) {
            Optional<UsuarioPorRol> byId = activeAssignments.stream()
                    .filter(upr -> Objects.equals(upr.getRol().getId(), preferredRoleId))
                    .findFirst();
            if (byId.isPresent()) return byId.get();
        }
        if (preferredRoleName != null && !preferredRoleName.isBlank()) {
            Optional<UsuarioPorRol> byName = activeAssignments.stream()
                    .filter(upr -> preferredRoleName.equals(upr.getRol().getName()))
                    .findFirst();
            if (byName.isPresent()) return byName.get();
        }

        return activeAssignments.stream()
                .min(Comparator.comparingInt(upr -> switch (upr.getRol().getPurpose()) {
                    case PLATFORM_ADMIN -> 0;
                    case COMPANY_ADMIN -> 1;
                    default -> 2;
                }))
                .orElse(null);
    }

    /** Usuario.activo es GLOBAL a la persona (una sola fila Usuario compartida entre
     * empresas), pero "activo/inactivo" en el sentido de negocio es un dato POR EMPRESA
     * (Empleado.estado o Apoderado.estado). Sin este chequeo, resolveActiveAssignment solo
     * miraba si el ROL seguia existiendo/habilitado - nunca si la persona seguia siendo
     * empleado o cliente activo de esa empresa en particular. Eso permitia que reactivar a
     * alguien en la Empresa B (lo que vuelve a poner Usuario.activo=true) le devolviera de
     * paso el acceso al rol de la Empresa A, aunque ahi su Empleado siguiera con estado=false.
     * Sin fila de Empleado NI de Apoderado en esa empresa (ej. rol PLATFORM_ADMIN, sin
     * empresa asociada) no hay membresia que validar, asi que no bloquea. */
    private boolean isRoleOperable(Integer userId, UsuarioPorRol assignment) {
        Company company = assignment.getCompany();
        if (company == null) {
            return true;
        }
        Integer companyId = company.getId();
        boolean esEmpleado = empleadoRepository.existsByUserIdAndCompanyId(userId, companyId);
        boolean esApoderado = apoderadoRepository.existsByUserIdAndCompanyId(userId, companyId);
        if (!esEmpleado && !esApoderado) {
            return true;
        }
        // Cada rol depende de SU relación: un rol de personal necesita un empleado activo y uno de cliente, un
        // cliente activo. Quien está suspendido como empleado pero sigue siendo cliente entra solo como cliente.
        veterinaria.vargasvet.domain.enums.RoleScope scope = assignment.getRol().getScope();
        boolean empleadoActivo = empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(userId, companyId);
        boolean apoderadoActivo = apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(userId, companyId);
        if (scope == veterinaria.vargasvet.domain.enums.RoleScope.STAFF) {
            return empleadoActivo;
        }
        if (scope == veterinaria.vargasvet.domain.enums.RoleScope.CLIENT) {
            return apoderadoActivo;
        }
        return empleadoActivo || apoderadoActivo;
    }

    /** Cada empresa tiene su propia credencial (contraseña, password_changed,
     * credentials_version) - companyId null resuelve la credencial "global" del
     * SuperAdmin, sin empresa asociada. */
    private Optional<veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial> resolveCredencial(
            Integer usuarioId, Integer companyId) {
        return companyId == null
                ? credencialRepository.findByUsuarioIdAndCompanyIsNull(usuarioId)
                : credencialRepository.findByUsuarioIdAndCompanyId(usuarioId, companyId);
    }

    /** Usado solo para el primer establecimiento de contraseña (correo de bienvenida):
     * una persona recien creada tiene exactamente una credencial, la de la primera
     * empresa a la que se unió - no hace falta que el token de verificación cargue
     * el companyId porque en ese momento es inequívoco. */
    private Optional<veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial> resolveSingleCredencial(
            Integer usuarioId) {
        List<veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial> todas =
                credencialRepository.findAllByUsuarioId(usuarioId);
        return todas.size() == 1 ? Optional.of(todas.get(0)) : Optional.empty();
    }

    private boolean anyCredencialPasswordChanged(Integer usuarioId) {
        return credencialRepository.findAllByUsuarioId(usuarioId).stream()
                .anyMatch(veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial::isPasswordChanged);
    }

    private String createAccessToken(Usuario usuario, UsuarioPorRol activeAssignment,
                                     List<String> activeRoles,
                                     Integer companyId, long credentialsVersion, String sessionId) {
        if (activeAssignment == null) {
            return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles,
                    companyId, null, null, null, 0L, credentialsVersion, sessionId);
        }
        var role = activeAssignment.getRol();
        return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles,
                companyId, role.getId(), role.getScope(), role.getPurpose(), role.getPermissionVersion(),
                credentialsVersion, sessionId);
    }

    private AuthResponse buildSessionResponse(Usuario usuario, UsuarioPorRol activeAssignment, Integer companyId,
                                              Company company, UsuarioEmpresaCredencial credencial,
                                              List<String> assignedRoles, List<String> activeRoles,
                                              List<Object> menu, List<String> permissions) {
        AuthResponse response = new AuthResponse();
        response.setRoles(activeRoles);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, companyId));
        response.setCompanyId(companyId);
        response.setCompanyName(company != null ? company.getName() : null);
        response.setCompanyLogoUrl(company != null ? company.getLogoUrl() : null);
        response.setCompanyColorPrimario(company != null ? company.getColorPrimario() : null);
        response.setCompanySlug(company != null ? company.getSlug() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario, companyId));
        response.setPasswordChanged(isPasswordPromptSatisfied(credencial));
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setEmpleadoId(resolveActiveEmpleadoId(usuario, companyId));
        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);
        return response;
    }

    private void populateActiveRole(AuthResponse response, UsuarioPorRol activeAssignment) {
        if (activeAssignment == null) return;
        var role = activeAssignment.getRol();
        response.setActiveRoleId(role.getId());
        response.setActiveRoleName(role.getName());
        response.setActiveRoleScope(role.getScope());
        response.setActiveRolePurpose(role.getPurpose());
        response.setPermissionVersion(role.getPermissionVersion());
    }

    /** Los roles disponibles para cambiar (dropdown del navbar) son SOLO los de la
     * empresa de la sesion actual (o solo los sin empresa, para SuperAdmin) - nunca los
     * de otra empresa. Sin este filtro, una persona con roles en dos empresas (dato de
     * antes del aislamiento total entre empresas, ver V74) veia en el selector un rol
     * de la OTRA empresa mientras estaba en esta - el cambio de rol lo rechazaba
     * igual, pero ya mostrar la opcion filtraba que esa persona tiene cuenta ahi. */
    private List<AssignedRoleResponse> toAvailableRoles(Usuario usuario, Integer companyId) {
        return usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        ? upr.getCompany() == null
                        : upr.getCompany() != null && companyId.equals(upr.getCompany().getId()))
                .filter(upr -> companyId == null || isRoleOperable(usuario.getId(), upr))
                .map(UsuarioPorRol::getRol)
                .filter(veterinaria.vargasvet.domain.entity.Role::isActivo)
                .sorted(Comparator.comparing(veterinaria.vargasvet.domain.entity.Role::getName))
                .map(role -> AssignedRoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .scope(role.getScope())
                        .purpose(role.getPurpose())
                        .build())
                .toList();
    }

    private String resolveNombreCompleto(Usuario usuario) {
        if (usuario.getNombre() != null) {
            return usuario.getNombre() + (usuario.getApellido() != null ? " " + usuario.getApellido() : "");
        }
        return usuario.getEmail();
    }

    private String resolveUserType(Usuario usuario, Integer companyId) {
        boolean isSuperAdmin = usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        ? upr.getCompany() == null
                        : upr.getCompany() != null && companyId.equals(upr.getCompany().getId()))
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);
        if (isSuperAdmin) return "SUPER_ADMIN";
        if (companyId != null
                && empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(usuario.getId(), companyId)) {
            return "EMPLEADO";
        }
        return "USUARIO";
    }

    /** El empleado de la respuesta pertenece siempre a la empresa firmada en la sesión. */
    private Integer resolveActiveEmpleadoId(Usuario usuario, Integer companyId) {
        if (companyId == null) return null;
        return empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(usuario.getId(), companyId)
                .map(empleado -> Math.toIntExact(empleado.getId()))
                .orElse(null);
    }
}
