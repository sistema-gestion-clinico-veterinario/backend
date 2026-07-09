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
import veterinaria.vargasvet.dto.response.MenuItemDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import veterinaria.vargasvet.domain.entity.PasswordResetToken;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;

import java.util.*;
import java.util.stream.Collectors;
import veterinaria.vargasvet.service.MenuBuilderService;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements veterinaria.vargasvet.service.UsuarioService {

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

    @Override
    @Transactional
    public UserProfileDTO register(UserRegistrationDTO registrationDTO) {
        if (usuarioRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        Usuario usuario = userMapper.toEntity(registrationDTO);
        usuario.setVerificationToken(UUID.randomUUID().toString());
        usuario.setEmailVerified(false);
        usuario.setActivo(false);

        Usuario saved = usuarioRepository.save(usuario);

        roleRepository.findFirstByName("ROLE_VETERINARIO").ifPresent(role -> {
            UsuarioPorRol upr = new UsuarioPorRol();
            upr.setUsuario(saved);
            upr.setRol(role);
            usuarioPorRolRepository.save(upr);
        });

        sendVerificationEmail(saved);

        return userMapper.toProfileDTO(saved);
    }

    private void sendVerificationEmail(Usuario usuario) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", usuario.getEmail());
            model.put("companyName", companyName);
            model.put("companyLogo", companyLogo);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyAddress", companyAddress);
            model.put("verificationLink", frontendVerifyUrl + usuario.getVerificationToken());

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
        Usuario usuario = usuarioRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido"));

        if (usuario.isEmailVerified() || usuario.isPasswordChanged()) {
            usuario.setVerificationToken(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Inicia sesion o recupera tu contrasena.");
        }

        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void setupAccount(String token, String password) {
        Usuario usuario = usuarioRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificacion invalido o expirado"));

        if (usuario.isEmailVerified() || usuario.isPasswordChanged()) {
            usuario.setVerificationToken(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Usa recuperacion de contrasena si necesitas cambiarla.");
        }

        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setPasswordChanged(true);
        usuario.setEmailVerified(true);
        usuario.setActivo(true);
        usuario.setVerificationToken(null);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void resendVerificationToken(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (usuario.isEmailVerified() || usuario.isPasswordChanged() || usuario.isActivo()) {
            usuario.setVerificationToken(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("La cuenta ya fue activada. Usa recuperacion de contrasena si necesitas cambiarla.");
        }

        String newToken = UUID.randomUUID().toString();
        usuario.setVerificationToken(newToken);
        usuarioRepository.save(usuario);

        sendVerificationEmail(usuario);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginDTO loginDTO) {
        String email = loginDTO.getEmail() != null ? loginDTO.getEmail().trim() : null;
        loginDTO.setEmail(email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (!usuario.isEmailVerified()) {
            throw new DisabledException("Tu cuenta aún no ha sido verificada. Por favor, revisa tu correo electrónico.");
        }

        if (!usuario.isActivo()) {
            throw new DisabledException("La cuenta está suspendida");
        }

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        String activeRole = resolveActiveRole(assignedRoles);

        boolean activeRoleActivo = usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getRol().getName().equals(activeRole))
                .anyMatch(upr -> upr.getRol().isActivo());
        if (!activeRoleActivo && !assignedRoles.isEmpty()) {
            throw new DisabledException("Tu rol activo se encuentra desactivado. Contacta al administrador.");
        }

        List<String> activeRolesList = activeRole != null
                ? java.util.Collections.singletonList(activeRole)
                : java.util.Collections.emptyList();

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRole));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRole);
        String jwt = tokenProvider.createToken(usuario.getEmail(), activeRolesList, permissions, companyId);
        String refreshToken = createRefreshToken(usuario, activeRole, Instant.now());

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
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
    public AuthResponse switchRole(String email, String roleName) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<String> assignedRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        if (!assignedRoles.contains(roleName)) {
            throw new IllegalArgumentException("El usuario no tiene asignado el rol solicitado");
        }

        boolean roleActivo = usuario.getUsuariosPorRol().stream()
                .filter(upr -> upr.getRol().getName().equals(roleName))
                .anyMatch(upr -> upr.getRol().isActivo());
        if (!roleActivo) {
            throw new IllegalArgumentException("El rol seleccionado se encuentra desactivado");
        }

        List<String> activeRolesList = java.util.Collections.singletonList(roleName);
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        List<Object> menu = new java.util.ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), roleName));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), roleName);
        String jwt = tokenProvider.createToken(usuario.getEmail(), activeRolesList, permissions, companyId);
        Instant sessionStartedAt = refreshTokenRepository.findFirstByUsuarioOrderByExpiryDateDesc(usuario)
                .map(RefreshToken::getSessionStartedAt)
                .orElse(Instant.now());
        refreshTokenRepository.deleteByUsuario(usuario);
        String refreshToken = createRefreshToken(usuario, roleName, sessionStartedAt);

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(assignedRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return userMapper.toProfileDTO(usuario);
    }

    @Override
    @Transactional
    public void suspendAccount(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

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

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setPasswordChanged(true);
        usuarioRepository.save(usuario);

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
    public void resetPassword(veterinaria.vargasvet.dto.request.AdminChangePasswordRequest dto) {
        Usuario usuario;
        if (dto.getUserId() != null) {
            usuario = usuarioRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        } else if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            usuario = usuarioRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con el email: " + dto.getEmail()));
        } else {
            throw new IllegalArgumentException("Debe proporcionar el ID de usuario o el correo electrónico");
        }

        // Aislamiento de datos: Si no es SUPER_ADMIN, solo puede resetear a usuarios de su misma empresa
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para cambiar la contraseña de este usuario");
            }
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setPasswordChanged(true);
        usuarioRepository.save(usuario);

        // Registrar log de auditoría para reset de contraseña administrativa
        auditLogService.log(
            "RESET_CONTRASENA",
            "Seguridad",
            "El administrador reseteó la contraseña del usuario: " + usuario.getEmail()
        );

        // Enviar notificación informativa
        sendPasswordChangeNotification(usuario);
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
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + request.getEmail()));

        // Delete any existing reset token for this user
        passwordResetTokenRepository.deleteByUsuario(usuario);

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .usuario(usuario)
                .expiryDate(veterinaria.vargasvet.util.AppClock.now().plusHours(24)) // 24 hours validity
                .build();
        
        passwordResetTokenRepository.save(resetToken);

        // Send email
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("usuario", resolveNombreCompleto(usuario));
            model.put("companyName", companyName);
            model.put("companyEmail", companyEmail);
            model.put("companyPhone", companyPhone);
            model.put("companyLogo", companyLogo);
            
            // Construct the reset URL pointing to the frontend
            String resetUrl = appUrl + "/reset-password?token=" + token;
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
                null, 
                companyName, 
                "SOLICITAR_RESTABLECER_PASSWORD", 
                "Seguridad", 
                "El usuario ha solicitado restablecer su contraseña.",
                null
            );
        } catch (Exception e) {
            System.err.println("[WARNING] No se pudo enviar el correo de recuperación a " + usuario.getEmail() + ": " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPasswordWithToken(veterinaria.vargasvet.dto.request.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("El token es inválido o no existe."));

        if (resetToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("El token ha expirado. Por favor solicite uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuario.setPasswordChanged(true);
        usuarioRepository.save(usuario);

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
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> !resetToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.now()))
                .orElse(false);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElse(null);

        if (refreshToken == null) {
            Optional<String> emailOpt = tokenProvider.getEmailFromToken(token);
            if (emailOpt.isPresent()) {
                throw new BadCredentialsException("Refresh token inválido o expirado. Por favor, inicie sesión nuevamente.");
            }
            throw new BadCredentialsException("Refresh token inválido o expirado. Por favor, inicie sesión nuevamente.");
        }

        if (refreshToken.getExpiryDate().isBefore(veterinaria.vargasvet.util.AppClock.instantNow())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expirado. Por favor, inicie sesión nuevamente.");
        }

        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        if (refreshToken.getSessionStartedAt().plusSeconds(absoluteTimeoutSeconds).isBefore(now)) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("La sesión ha expirado. Por favor, inicie sesión nuevamente.");
        }

        Usuario usuario = refreshToken.getUsuario();

        boolean esSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> "ROLE_SUPER_ADMIN".equals(upr.getRol().getName()));

        if (!usuario.isActivo()) {
            refreshTokenRepository.delete(refreshToken);
            throw new DisabledException("La cuenta está suspendida");
        }

        if (!esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo()) {
            refreshTokenRepository.delete(refreshToken);
            throw new DisabledException("La empresa está desactivada. Contacta al administrador del sistema.");
        }

        List<String> userRoles = usuario.getUsuariosPorRol().stream()
                .map(upr -> upr.getRol().getName())
                .collect(Collectors.toList());

        String activeRole = tokenProvider.getActiveRoleFromRefreshToken(token)
                .filter(userRoles::contains)
                .orElseGet(() -> resolveActiveRole(userRoles));
        List<String> activeRolesList = activeRole != null
                ? Collections.singletonList(activeRole)
                : Collections.emptyList();

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;

        List<Object> menu = new ArrayList<>(menuBuilderService.construirMenuJerarquico(usuario.getId(), activeRole));
        List<String> permissions = menuBuilderService.construirPermissions(usuario.getId(), activeRole);
        String newJwt = tokenProvider.createToken(usuario.getEmail(), activeRolesList, permissions, companyId);

        String newRefreshToken = createRefreshToken(usuario, activeRole, refreshToken.getSessionStartedAt());
        refreshTokenRepository.delete(refreshToken);

        AuthResponse response = new AuthResponse();
        response.setToken(newJwt);
        response.setRefreshToken(newRefreshToken);
        response.setRoles(activeRolesList);
        response.setAssignedRoles(userRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(usuario.getEmpleado() != null ? Math.toIntExact(usuario.getEmpleado().getId()) : null);
        response.setMenu(menu);
        response.setPermissions(permissions);

        return response;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    private String createRefreshToken(Usuario usuario, String activeRole, Instant sessionStartedAt) {
        String token = tokenProvider.createRefreshToken(usuario.getEmail(), activeRole);
        Instant now = veterinaria.vargasvet.util.AppClock.instantNow();
        Instant expiryDate = now.plusSeconds(604800); // 7 días

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(token)
                .expiryDate(expiryDate)
                .sessionStartedAt(sessionStartedAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String resolveActiveRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        List<String> priority = List.of(
                "ROLE_SUPER_ADMIN",
                "ROLE_ADMIN"
        );

        return priority.stream()
                .filter(roles::contains)
                .findFirst()
                .orElse(roles.get(0));
    }

    private String resolveNombreCompleto(Usuario usuario) {
        if (usuario.getNombre() != null) {
            return usuario.getNombre() + (usuario.getApellido() != null ? " " + usuario.getApellido() : "");
        }
        return usuario.getEmail();
    }

    private String resolveUserType(Usuario usuario) {
        boolean isSuperAdmin = usuario.getUsuariosPorRol().stream()
                .anyMatch(upr -> upr.getRol().getName().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return "SUPER_ADMIN";
        if (usuario.getEmpleado() != null) return "EMPLEADO";
        return "USUARIO";
    }
}
