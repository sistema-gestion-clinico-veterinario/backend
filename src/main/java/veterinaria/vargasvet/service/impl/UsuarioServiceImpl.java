package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
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

    private static final String DUMMY_BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO5LwR8mH3eQfPJfQZpD1fM9L0f.R8j6u";

    private final UsuarioRepository usuarioRepository;
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
    private final SessionSecurityService sessionSecurityService;
    private final SharedRateLimitService sharedRateLimitService;
    private final AuthenticationAuditService authenticationAuditService;
    private final PasswordPolicyService passwordPolicyService;

    @Value("${app.frontend.verify-url}")
    private String frontendVerifyUrl;

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

    @Value("${jwt.refresh-validity-in-seconds:604800}")
    private long refreshValiditySeconds;

    @Value("${security.password-reset-validity-minutes:60}")
    private long passwordResetValidityMinutes;

    @Value("${app.rate-limit.login-per-account-per-15-minutes:8}")
    private int loginPerAccountPerWindow;

    @Value("${app.rate-limit.recovery-per-account-per-hour:3}")
    private int recoveryPerAccountPerHour;

    @Override
    @Transactional
    public UserProfileDTO register(UserRegistrationDTO registrationDTO) {
        registrationDTO.setEmail(normalizeSecurityIdentifier(registrationDTO.getEmail()));
        if (usuarioRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        passwordPolicyService.validate(registrationDTO.getPassword(), registrationDTO.getEmail(),
                registrationDTO.getNombre(), registrationDTO.getApellido());
        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        Usuario usuario = userMapper.toEntity(registrationDTO);
        String verificationToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(24));
        usuario.setEmailVerified(false);
        usuario.setActivo(false);
        if (registrationDTO.getCompanyId() != null) {
            usuario.setCompany(companyRepository.findById(registrationDTO.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        }

        Usuario saved = usuarioRepository.save(usuario);

        sendVerificationEmail(saved, verificationToken);

        return userMapper.toProfileDTO(saved);
    }

    private void sendVerificationEmail(Usuario usuario, String verificationToken) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", usuario.getEmail());
            model.put("companyName", companyName);
            model.put("companyLogo", companyLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("verificationLink", frontendVerifyUrl + verificationToken);

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Bienvenido a " + companyName + " - Activa tu cuenta",
                    model
            );

            emailService.sendEmail(mail, "email/welcome-template");
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de verificación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido"));
        assertVerificationTokenNotExpired(usuario);

        if (usuario.isEmailVerified() || usuario.isPasswordChanged()) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Inicia sesion o recupera tu contrasena.");
        }

        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuario.setVerificationTokenExpiresAt(null);
        usuarioRepository.save(usuario);
        authenticationAuditService.record(usuario, "ACTIVAR_CUENTA",
                "La cuenta fue activada mediante confirmación del correo.");
    }

    @Override
    @Transactional
    public void setupAccount(String token, String password) {
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido o expirado"));
        assertVerificationTokenNotExpired(usuario);

        if (usuario.isEmailVerified() || usuario.isPasswordChanged()) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Usa recuperacion de contrasena si necesitas cambiarla.");
        }

        passwordPolicyService.validate(password, usuario.getEmail(), usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(password, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setPasswordChanged(true);
        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuario.setVerificationTokenExpiresAt(null);
        usuarioRepository.save(usuario);
        authenticationAuditService.record(usuario, "CONFIGURAR_CREDENCIALES",
                "El usuario estableció su contraseña inicial y activó la cuenta.");
    }

    @Override
    @Transactional
    public void resendVerificationToken(String email) {
        email = normalizeSecurityIdentifier(email);
        sharedRateLimitService.enforce("verification-account", email,
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        // Respuesta uniforme: no revelar si la cuenta existe o ya fue activada.
        if (usuario == null) {
            passwordEncoder.matches("verification-probe", DUMMY_BCRYPT_HASH);
            return;
        }

        if (usuario.isEmailVerified() || usuario.isPasswordChanged() || usuario.isActivo()) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            return;
        }

        String newToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(newToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(24));
        usuarioRepository.save(usuario);

        sendVerificationEmail(usuario, newToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginDTO loginDTO) {
        String email = normalizeSecurityIdentifier(loginDTO.getEmail());
        loginDTO.setEmail(email);
        sharedRateLimitService.enforce("login-account", normalizeSecurityIdentifier(email),
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            passwordEncoder.matches(loginDTO.getPassword(), DUMMY_BCRYPT_HASH);
            authenticationAuditService.recordLoginFailure(null, email, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            authenticationAuditService.recordLoginFailure(usuario, email, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (passwordEncoder.upgradeEncoding(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(loginDTO.getPassword()));
        }

        if (!usuario.isEmailVerified()) {
            authenticationAuditService.recordLoginFailure(usuario, email, "cuenta no habilitada");
            throw new DisabledException("Tu cuenta aún no ha sido verificada. Por favor, revisa tu correo electrónico.");
        }

        if (!usuario.isActivo()) {
            authenticationAuditService.recordLoginFailure(usuario, email, "cuenta no habilitada");
            throw new DisabledException("La cuenta está suspendida");
        }

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);
        if (!esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo()) {
            throw new DisabledException("Acceso denegado. La empresa asociada a tu usuario esta inactiva. Contacta al administrador.");
        }

        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, null, null);
        if (activeAssignment == null && !assignedRoles.isEmpty()) {
            throw new DisabledException("Tu rol activo se encuentra desactivado. Contacta al administrador.");
        }
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;

        List<String> activeRolesList = activeRole != null
                ? java.util.Collections.singletonList(activeRole)
                : java.util.Collections.emptyList();

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId);
        String refreshToken = createRefreshToken(usuario, activeAssignment, Instant.now(), UUID.randomUUID().toString());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario));
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setCompanyLogoUrl(usuario.getCompany() != null ? usuario.getCompany().getLogoUrl() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(
                usuario.getEmpleado() != null
                        ? Math.toIntExact(usuario.getEmpleado().getId())
                        : null
        );
        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        // Registrar log de auditoría para Login
        auditLogService.log(
            usuario.getEmail(),
            activeRole,
            companyId,
            usuario.getCompany() != null ? usuario.getCompany().getName() : null,
            "LOGIN_EXITOSO",
            "Seguridad",
            "Inicio de sesión exitoso del usuario " + usuario.getEmail() + " con rol activo " + activeRole,
            null
        );

        return response;
    }

    @Override
    @Transactional
    public AuthResponse switchRole(String email, Integer roleId) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, roleId, null);
        if (activeAssignment == null) {
            throw new IllegalArgumentException("El rol seleccionado se encuentra desactivado");
        }

        String roleName = activeAssignment.getRol().getName();

        List<String> activeRolesList = java.util.Collections.singletonList(roleName);
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        Integer activeRoleId = activeAssignment.getRol().getId();
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId);
        Instant sessionStartedAt = refreshTokenRepository.findFirstByUsuarioOrderByExpiryDateDesc(usuario)
                .map(RefreshToken::getSessionStartedAt)
                .orElse(Instant.now());
        sessionSecurityService.invalidateAllSessions(usuario);
        String refreshToken = createRefreshToken(usuario, activeAssignment, sessionStartedAt, UUID.randomUUID().toString());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario));
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setCompanyLogoUrl(usuario.getCompany() != null ? usuario.getCompany().getLogoUrl() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(
                usuario.getEmpleado() != null
                        ? Math.toIntExact(usuario.getEmpleado().getId())
                        : null
        );

        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        // Registrar log de auditoría para cambio de rol
        auditLogService.log(
            usuario.getEmail(),
            roleName,
            companyId,
            usuario.getCompany() != null ? usuario.getCompany().getName() : null,
            "CAMBIO_ROL",
            "Seguridad",
            "Cambio de rol activo del usuario a " + roleName,
            null
        );

        return response;
    }

    @Override
    public UserProfileDTO getProfile(Integer id) {
        Usuario usuario = findManageableUser(id);
        return userMapper.toProfileDTO(usuario);
    }

    @Override
    @Transactional
    public void suspendAccount(Integer id) {
        Usuario usuario = findManageableUser(id);
        if (Objects.equals(SecurityUtils.getCurrentUserId(), usuario.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No puede suspender su propia cuenta");
        }
        usuario.setActivo(false);
        sessionSecurityService.invalidateAllSessions(usuario);

        auditLogService.log(
            "SUSPENSION_CUENTA",
            "Seguridad",
            "Se suspendió administrativamente la cuenta del usuario: " + usuario.getEmail()
        );
    }

    @Override
    @Transactional
    public void changePassword(String email, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getOldPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        passwordPolicyService.validate(dto.getNewPassword(), usuario.getEmail(),
                usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(dto.getNewPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setPasswordChanged(true);
        sessionSecurityService.invalidateAllSessions(usuario);

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
        Usuario usuario;
        if (dto.getUserId() != null) {
            usuario = findManageableUser(dto.getUserId());
        } else if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            usuario = findManageableUser(normalizeSecurityIdentifier(dto.getEmail()));
        } else {
            throw new IllegalArgumentException("Debe proporcionar el ID de usuario o el correo electrónico");
        }

        if (!usuario.isActivo() || !usuario.isEmailVerified()) {
            throw new IllegalStateException(
                    "La cuenta todavía no está habilitada; debe completar primero su activación");
        }

        sharedRateLimitService.enforce("admin-reset-account", normalizeSecurityIdentifier(usuario.getEmail()),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));

        issuePasswordResetToken(usuario, "SOLICITAR_RESET_ADMINISTRATIVO",
                "Un administrador solicitó el restablecimiento de acceso del usuario");
    }

    private void sendPasswordChangeNotification(Usuario usuario) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", resolveNombreCompleto(usuario));
            model.put("email", usuario.getEmail());
            model.put("companyName", companyName);
            model.put("companyLogo", companyLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("appUrl", appUrl);

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Notificación de Cambio de Contraseña - " + companyName,
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
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);
        if (usuario == null || !usuario.isActivo() || !usuario.isEmailVerified()) {
            return;
        }

        issuePasswordResetToken(usuario, "SOLICITAR_RESTABLECER_PASSWORD",
                "El usuario solicitó restablecer su contraseña");
    }

    private void issuePasswordResetToken(Usuario usuario, String action, String detail) {
        passwordResetTokenRepository.deleteByUsuario(usuario);

        String token = SecurityTokenUtils.generate();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashToken(token))
                .usuario(usuario)
                .expiryDate(veterinaria.vargasvet.util.AppClock.now().plusMinutes(passwordResetValidityMinutes))
                .build();
        
        passwordResetTokenRepository.save(resetToken);

        try {
            Map<String, Object> model = new HashMap<>();
            model.put("usuario", resolveNombreCompleto(usuario));
            model.put("companyName", companyName);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyLogo", companyLogo);
            
            String resetUrl = appUrl + "/reset-password#token=" + token;
            model.put("resetUrl", resetUrl);

            Mail mail = emailService.createMail(
                    usuario.getEmail(),
                    "Restablecer Contraseña - " + companyName,
                    model
            );
            emailService.sendEmail(mail, "email/forgot-password-template");
            
            auditLogService.log(
                usuario.getEmail(), 
                "USER", 
                usuario.getCompany() != null ? usuario.getCompany().getId() : null,
                usuario.getCompany() != null ? usuario.getCompany().getName() : companyName,
                action,
                "Seguridad", 
                detail,
                null
            );
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de recuperación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenForUpdate(hashToken(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException("El token es inválido o no existe."));

        if (resetToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El token ha expirado. Por favor solicite uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        passwordPolicyService.validate(request.getNewPassword(), usuario.getEmail(),
                usuario.getNombre(), usuario.getApellido());
        if (passwordEncoder.matches(request.getNewPassword(), usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuario.setPasswordChanged(true);
        sessionSecurityService.invalidateAllSessions(usuario);

        passwordResetTokenRepository.delete(resetToken);
        
        auditLogService.log(
            usuario.getEmail(), 
            "USER", 
            null, 
            companyName, 
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
        if (tokenMismatch || refreshToken.getUsedAt() != null || refreshToken.getRevokedAt() != null) {
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

        if (tokenDetails.credentialsVersion() != usuario.getCredentialsVersion()) {
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

        if (!esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new DisabledException("La empresa está desactivada. Contacta al administrador del sistema.");
        }

        List<String> userRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = resolveActiveAssignment(
                usuario, tokenDetails.activeRoleId(), tokenDetails.activeRole());
        if (activeAssignment == null && !userRoles.isEmpty()) {
            revokeFamily(refreshToken.getFamilyId(), now);
            throw new DisabledException("El rol de la sesión ya no está disponible");
        }
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;
        List<String> activeRolesList = activeRole != null
                ? Collections.singletonList(activeRole)
                : Collections.emptyList();

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String newJwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId);

        refreshToken.setUsedAt(now);
        refreshToken.setRevokedAt(now);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = createRefreshToken(usuario, activeAssignment,
                refreshToken.getSessionStartedAt(), refreshToken.getFamilyId());

        AuthResponse response = new AuthResponse();
        response.setToken(newJwt);
        response.setRefreshToken(newRefreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(userRoles);
        response.setAvailableRoles(toAvailableRoles(usuario));
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setCompanyLogoUrl(usuario.getCompany() != null ? usuario.getCompany().getLogoUrl() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(usuario.getEmpleado() != null ? Math.toIntExact(usuario.getEmpleado().getId()) : null);
        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        return response;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hashToken(refreshToken))
                .ifPresent(token -> revokeFamily(token.getFamilyId(), veterinaria.vargasvet.util.AppClock.instantNow()));
    }

    private String createRefreshToken(Usuario usuario, UsuarioPorRol activeAssignment, Instant sessionStartedAt,
                                      String familyId) {
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;
        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        String token = tokenProvider.createRefreshToken(usuario.getEmail(), activeRole, activeRoleId,
                familyId, usuario.getCredentialsVersion());
        TokenProvider.RefreshTokenDetails details = tokenProvider.getRefreshTokenDetails(token);
        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        Instant expiryDate = now.plusSeconds(refreshValiditySeconds);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
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

    private void assertVerificationTokenNotExpired(Usuario usuario) {
        if (usuario.getVerificationTokenExpiresAt() == null
                || usuario.getVerificationTokenExpiresAt().isBefore(veterinaria.vargasvet.util.AppClock.now())) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("El enlace de activación es inválido o expiró");
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
        return usuarioRepository.findByIdAndCompanyId(userId, companyId)
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
        return usuarioRepository.findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private UsuarioPorRol resolveActiveAssignment(Usuario usuario, Integer preferredRoleId, String preferredRoleName) {
        List<UsuarioPorRol> activeAssignments = usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getRol().isActivo())
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

    private String createAccessToken(Usuario usuario, UsuarioPorRol activeAssignment,
                                     List<String> activeRoles, List<String> permissions,
                                     Integer companyId) {
        if (activeAssignment == null) {
            return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles, permissions,
                    companyId, null, null, null, 0L, usuario.getCredentialsVersion());
        }
        var role = activeAssignment.getRol();
        return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles, permissions,
                companyId, role.getId(), role.getScope(), role.getPurpose(), role.getPermissionVersion(),
                usuario.getCredentialsVersion());
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

    private List<AssignedRoleResponse> toAvailableRoles(Usuario usuario) {
        return usuario.getUsuariosPorRol().stream()
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

    private String resolveUserType(Usuario usuario) {
        boolean isSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN);
        if (isSuperAdmin) return "SUPER_ADMIN";
        if (usuario.getEmpleado() != null) return "EMPLEADO";
        return "USUARIO";
    }
}
