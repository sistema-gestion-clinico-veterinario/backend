package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
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
    private final AuthenticationAuditService authenticationAuditService;
    private final PasswordPolicyService passwordPolicyService;
    private final veterinaria.vargasvet.service.LegalDocumentService legalDocumentService;
    private final UsuarioContactoService contactoService;

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

    @Value("${security.verification-token-validity-hours:24}")
    private long verificationTokenValidityHours;

    @Value("${app.rate-limit.login-per-account-per-15-minutes:8}")
    private int loginPerAccountPerWindow;

    @Value("${app.rate-limit.recovery-per-account-per-hour:3}")
    private int recoveryPerAccountPerHour;

    @Override
    @Transactional
    public UserProfileDTO register(UserRegistrationDTO registrationDTO) {
        registrationDTO.setEmail(normalizeSecurityIdentifier(registrationDTO.getEmail()));
        registrationDTO.setUsername(normalizeSecurityIdentifier(registrationDTO.getUsername()));

        Company company = null;
        if (registrationDTO.getCompanyId() != null) {
            company = companyRepository.findById(registrationDTO.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        }
        Integer companyIdToUse = company != null ? company.getId() : null;

        // Aislamiento total entre empresas: los duplicados (correo, username) solo se
        // validan dentro de la misma empresa (o entre cuentas sin empresa).
        boolean emailEnUso = companyIdToUse == null
                ? usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull(registrationDTO.getEmail())
                : usuarioRepository.existsByEmailIgnoreCaseAndCompanyId(registrationDTO.getEmail(), companyIdToUse);
        if (emailEnUso) {
            throw new IllegalArgumentException("El email ya está en uso");
        }
        boolean usernameEnUso = companyIdToUse == null
                ? usuarioRepository.existsByUsernameIgnoreCaseAndCompanyIsNull(registrationDTO.getUsername())
                : usuarioRepository.existsByUsernameIgnoreCaseAndCompanyId(registrationDTO.getUsername(), companyIdToUse);
        if (usernameEnUso) {
            throw new IllegalArgumentException("El usuario ya está en uso");
        }

        passwordPolicyService.validate(registrationDTO.getPassword(), registrationDTO.getEmail(),
                registrationDTO.getNombre(), registrationDTO.getApellido());
        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());
        Usuario usuario = userMapper.toEntity(registrationDTO);
        String verificationToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(verificationToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(verificationTokenValidityHours));
        usuario.setEmailVerified(false);
        usuario.setActivo(false);
        usuario.setCompany(company);

        Usuario saved = usuarioRepository.save(usuario);

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                new veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial();
        credencial.setUsuario(saved);
        credencial.setCompany(company);
        credencial.setPassword(encodedPassword);
        credencial.setPasswordChanged(false);
        credencial.setCreatedAt(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);
        contactoService.crear(saved, company, registrationDTO.getTelefono(), registrationDTO.getDireccion());

        sendVerificationEmail(saved, verificationToken);

        UserProfileDTO profile = userMapper.toProfileDTO(saved);
        profile.setTelefono(registrationDTO.getTelefono());
        profile.setDireccion(registrationDTO.getDireccion());
        return profile;
    }

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

    private void sendVerificationEmail(Usuario usuario, String verificationToken) {
        try {
            Map<String, Object> model = new HashMap<>(resolveCompanyBranding(usuario));
            model.put("nombre", usuario.getEmail());
            String slug = usuario.getCompany() != null ? usuario.getCompany().getSlug() : null;
            model.put("verificationLink", appUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                    "/auth/verify#token=" + verificationToken, slug));

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
    public void verifyEmail(String token) {
        Usuario usuario = usuarioRepository.findByVerificationTokenForUpdate(SecurityTokenUtils.hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido"));
        assertVerificationTokenNotExpired(usuario);

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId())) {
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

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId())) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Usa recuperacion de contrasena si necesitas cambiarla.");
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial = resolveSingleCredencial(usuario.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo determinar la credencial a configurar para este usuario"));

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

        if (usuario.isEmailVerified() || anyCredencialPasswordChanged(usuario.getId()) || usuario.isActivo()) {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
            return;
        }

        String newToken = SecurityTokenUtils.generate();
        usuario.setVerificationToken(SecurityTokenUtils.hash(newToken));
        usuario.setVerificationTokenExpiresAt(veterinaria.vargasvet.util.AppClock.now().plusHours(verificationTokenValidityHours));
        usuarioRepository.save(usuario);

        sendVerificationEmail(usuario, newToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginDTO loginDTO) {
        String username = normalizeSecurityIdentifier(loginDTO.getUsername());
        loginDTO.setUsername(username);
        String slug = loginDTO.getSlug() == null ? null : loginDTO.getSlug().trim().toLowerCase(Locale.ROOT);
        sharedRateLimitService.enforce("login-account", username,
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));

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

        return buildLoginResponse(usuario, company, credencial, username);
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
        String normalizedSlug = slug == null ? null : slug.trim().toLowerCase(Locale.ROOT);
        sharedRateLimitService.enforce("login-account", normalizedEmail,
                loginPerAccountPerWindow, java.time.Duration.ofMinutes(15));

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail).orElse(null);
        if (usuario == null) {
            authenticationAuditService.recordLoginFailure(null, normalizedEmail, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Company company;
        if (normalizedSlug != null) {
            company = companyRepository.findBySlug(normalizedSlug).orElse(null);
            if (company == null || !companyMembershipService.hasActiveMembership(usuario.getId(), company.getId())) {
                authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "credenciales inválidas");
                throw new BadCredentialsException("Credenciales inválidas");
            }
        } else {
            Set<Integer> activeCompanyIds = companyMembershipService.getActiveCompanyIds(usuario);
            company = activeCompanyIds.size() == 1
                    ? companyRepository.findById(activeCompanyIds.iterator().next()).orElse(null)
                    : null;
            if (company == null) {
                authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "credenciales inválidas");
                throw new BadCredentialsException("Credenciales inválidas");
            }
        }

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), company.getId()).orElse(null);
        if (credencial == null) {
            authenticationAuditService.recordLoginFailure(usuario, normalizedEmail, "credenciales inválidas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        credencial.setUltimoAcceso(veterinaria.vargasvet.util.AppClock.now());
        credencialRepository.save(credencial);

        return buildLoginResponse(usuario, company, credencial, normalizedEmail);
    }

    /** Cola compartida por login() (con contraseña) y loginWithGoogle() (sin contraseña):
     * una vez identificada la persona, la empresa y su credencial en esa empresa, el resto
     * del proceso (estado de cuenta, roles activos, emisión de JWT, auditoría) es idéntico. */
    private AuthResponse buildLoginResponse(Usuario usuario, Company company,
            veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial, String username) {
        if (!usuario.isEmailVerified()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada");
            throw new DisabledException("Tu cuenta aún no ha sido verificada. Por favor, revisa tu correo electrónico.");
        }

        if (!usuario.isActivo()) {
            authenticationAuditService.recordLoginFailure(usuario, username, "cuenta no habilitada");
            throw new DisabledException("La cuenta está suspendida");
        }

        if (!company.isActivo()) {
            throw new DisabledException("Acceso denegado. La empresa está inactiva. Contacta al administrador.");
        }

        // Solo los roles de ESTA empresa (la del slug) - si la persona
        // tambien trabaja o es cliente en otra empresa, esos roles no deben
        // verse ni activarse aqui.
        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getCompany() != null && upr.getCompany().getId().equals(company.getId()))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, null, null, company.getId());
        if (activeAssignment == null && !assignedRoles.isEmpty()) {
            throw new DisabledException("Tu rol activo se encuentra desactivado. Contacta al administrador.");
        }
        String activeRole = activeAssignment != null ? activeAssignment.getRol().getName() : null;

        List<String> activeRolesList = activeRole != null
                ? java.util.Collections.singletonList(activeRole)
                : java.util.Collections.emptyList();

        Integer companyId = company.getId();

        Integer activeRoleId = activeAssignment != null ? activeAssignment.getRol().getId() : null;
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId,
                credencial.getCredentialsVersion());
        String refreshToken = createRefreshToken(usuario, activeAssignment, Instant.now(), UUID.randomUUID().toString(),
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
        response.setCompanySlug(company.getSlug());
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(credencial.isPasswordChanged());
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setEmpleadoId(resolveActiveEmpleadoId(usuario));
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
            "Inicio de sesión exitoso del usuario " + usuario.getUsername() + " con rol activo " + activeRole,
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
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, null,
                credencial.getCredentialsVersion());
        String refreshToken = createRefreshToken(usuario, activeAssignment, Instant.now(), UUID.randomUUID().toString(),
                null, credencial.getCredentialsVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, null));
        response.setCompanyId(null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
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
    @Transactional
    public AuthResponse switchRole(Integer usuarioId, Integer roleId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // La empresa de la sesion ACTUAL (del JWT), no usuario.company (ambiguo
        // si la persona tiene relaciones activas en mas de una empresa) - solo
        // se puede cambiar de rol dentro de la misma empresa con la que se
        // inicio sesion, nunca "saltar" a un rol de otra empresa.
        Integer companyId = SecurityUtils.getCurrentCompanyId();

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .filter(upr -> companyId == null
                        || (upr.getCompany() != null && companyId.equals(upr.getCompany().getId())))
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        UsuarioPorRol activeAssignment = resolveActiveAssignment(usuario, roleId, null, companyId);
        if (activeAssignment == null) {
            throw new IllegalArgumentException("El rol seleccionado se encuentra desactivado");
        }

        String roleName = activeAssignment.getRol().getName();

        List<String> activeRolesList = java.util.Collections.singletonList(roleName);
        Company company = companyId != null ? companyRepository.findById(companyId).orElse(null) : null;

        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), companyId).orElseThrow(
                        () -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        Integer activeRoleId = activeAssignment.getRol().getId();
        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRoleId));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRoleId);
        String jwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId,
                credencial.getCredentialsVersion());
        Instant sessionStartedAt = refreshTokenRepository.findFirstByUsuarioOrderByExpiryDateDesc(usuario)
                .map(RefreshToken::getSessionStartedAt)
                .orElse(Instant.now());
        // Solo revoca las sesiones de ESTA empresa - cambiar de rol dentro de Vargas Vet
        // nunca debe desloguear a la persona de El Duke de Can si tiene sesión abierta ahí.
        sessionSecurityService.invalidateSessionsForCompany(usuario, company);
        String refreshToken = createRefreshToken(usuario, activeAssignment, sessionStartedAt, UUID.randomUUID().toString(),
                company, credencial.getCredentialsVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, companyId));
        response.setCompanyId(companyId);
        response.setCompanyName(company != null ? company.getName() : null);
        response.setCompanyLogoUrl(company != null ? company.getLogoUrl() : null);
        response.setCompanySlug(company != null ? company.getSlug() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(credencial.isPasswordChanged());
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setEmpleadoId(resolveActiveEmpleadoId(usuario));

        response.setMenu(menu);
        response.setPermissions(permissions);
        populateActiveRole(response, activeAssignment);

        // Registrar log de auditoría para cambio de rol
        auditLogService.log(
            usuario.getEmail(),
            roleName,
            companyId,
            company != null ? company.getName() : null,
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
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        UserProfileDTO profile = userMapper.toProfileDTO(usuario);
        profile.setTelefono(contactoService.telefono(usuario.getId(), companyId));
        profile.setDireccion(contactoService.direccion(usuario.getId(), companyId));
        return profile;
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
    public void changePassword(Integer usuarioId, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Cambia solo la credencial de la empresa con la que se inició la sesión actual -
        // nunca la de otra empresa donde la persona también tenga cuenta.
        Integer companyId = SecurityUtils.getCurrentCompanyId();
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

        // findManageableUser ya garantiza (salvo SuperAdmin) que el usuario pertenece a la
        // empresa activa del admin, así que esa es la credencial correcta a restablecer.
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Company company = companyId != null ? companyRepository.findById(companyId).orElse(null) : null;
        veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial credencial =
                resolveCredencial(usuario.getId(), companyId)
                        .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));

        issuePasswordResetToken(usuario, company, credencial, "SOLICITAR_RESET_ADMINISTRATIVO",
                "Un administrador solicitó el restablecimiento de acceso del usuario");
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
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail()).orElse(null);
        if (usuario == null || !usuario.isActivo() || !usuario.isEmailVerified()) {
            return;
        }

        // La empresa la resuelve el slug de la pantalla desde la que se pide el reset
        // (igual que en login) - determina cuál credencial se va a restablecer.
        String slug = request.getSlug() == null ? null : request.getSlug().trim().toLowerCase(Locale.ROOT);
        Company company = slug != null ? companyRepository.findBySlug(slug).orElse(null) : null;
        if (slug != null && (company == null || !companyMembershipService.hasActiveMembership(usuario.getId(), company.getId()))) {
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

        try {
            Map<String, Object> model = new HashMap<>(resolveCompanyBranding(usuario, company));
            model.put("usuario", resolveNombreCompleto(usuario));

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
        String newJwt = createAccessToken(usuario, activeAssignment, activeRolesList, permissions, companyId,
                credencial.getCredentialsVersion());

        refreshToken.setUsedAt(now);
        refreshToken.setRevokedAt(now);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = createRefreshToken(usuario, activeAssignment,
                refreshToken.getSessionStartedAt(), refreshToken.getFamilyId(), sessionCompany,
                credencial.getCredentialsVersion());

        AuthResponse response = new AuthResponse();
        response.setToken(newJwt);
        response.setRefreshToken(newRefreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(userRoles);
        response.setAvailableRoles(toAvailableRoles(usuario, companyId));
        response.setCompanyId(companyId);
        response.setCompanyName(sessionCompany != null ? sessionCompany.getName() : null);
        response.setCompanyLogoUrl(sessionCompany != null ? sessionCompany.getLogoUrl() : null);
        response.setCompanySlug(sessionCompany != null ? sessionCompany.getSlug() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(credencial.isPasswordChanged());
        response.setNeedsLegalAcceptance(legalDocumentService.hasPendingConsent(usuario.getId()));
        response.setLegalAcceptanceOverdue(legalDocumentService.isPastGracePeriod(usuario.getId()));
        response.setEmpleadoId(resolveActiveEmpleadoId(usuario));
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
                .ifPresent(token -> {
                    revokeFamily(token.getFamilyId(), veterinaria.vargasvet.util.AppClock.instantNow());
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
                .filter(upr -> isCompanyMembershipActive(usuario.getId(), upr.getCompany()))
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
    private boolean isCompanyMembershipActive(Integer userId, Company company) {
        if (company == null) {
            return true;
        }
        Integer companyId = company.getId();
        if (empleadoRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            return empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(userId, companyId);
        }
        if (apoderadoRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            return apoderadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(userId, companyId);
        }
        return true;
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
                                     List<String> activeRoles, List<String> permissions,
                                     Integer companyId, long credentialsVersion) {
        if (activeAssignment == null) {
            return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles, permissions,
                    companyId, null, null, null, 0L, credentialsVersion);
        }
        var role = activeAssignment.getRol();
        return tokenProvider.createToken(usuario.getId(), usuario.getEmail(), activeRoles, permissions,
                companyId, role.getId(), role.getScope(), role.getPurpose(), role.getPermissionVersion(),
                credentialsVersion);
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
        if (empleadoRepository.existsByUserIdAndEstadoTrue(usuario.getId())) return "EMPLEADO";
        return "USUARIO";
    }

    /** Como mucho hay una relacion laboral activa por usuario (indice
     * uq_empleado_activo_por_usuario), asi que esto es seguro sin importar cuantas
     * filas historicas de Empleado tenga el usuario. */
    private Integer resolveActiveEmpleadoId(Usuario usuario) {
        return empleadoRepository.findActiveByUserId(usuario.getId())
                .map(empleado -> Math.toIntExact(empleado.getId()))
                .orElse(null);
    }
}
