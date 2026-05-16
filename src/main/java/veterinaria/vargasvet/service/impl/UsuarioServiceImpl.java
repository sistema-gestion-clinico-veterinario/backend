package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.repository.RefreshTokenRepository;
import veterinaria.vargasvet.security.TokenProvider;
import veterinaria.vargasvet.domain.entity.RefreshToken;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;

import java.util.*;
import java.util.stream.Collectors;
import veterinaria.vargasvet.service.MenuService;
import veterinaria.vargasvet.dto.response.MenuDTO;
import veterinaria.vargasvet.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements veterinaria.vargasvet.service.UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final EmailService emailService;
    private final MenuService menuService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.company.name}")
    private String companyName;

    @Value("${app.company.logo}")
    private String companyLogo;

    @Override
    @Transactional
    public UserProfileDTO register(UserRegistrationDTO registrationDTO) {
        if (usuarioRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("El email ya está en uso");
        }

        registrationDTO.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        Usuario usuario = userMapper.toEntity(registrationDTO);
        
        roleRepository.findByName("ROLE_VETERINARIO")
                .ifPresent(role -> usuario.getRoles().add(role));

        String token = UUID.randomUUID().toString();
        usuario.setVerificationToken(token);
        usuario.setEmailVerified(false);
        usuario.setActivo(false);

        Usuario saved = usuarioRepository.save(usuario);

        sendVerificationEmail(saved);

        return userMapper.toProfileDTO(saved);
    }

    private void sendVerificationEmail(Usuario usuario) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("nombre", usuario.getEmail());
            model.put("companyName", companyName);
            model.put("companyLogo", companyLogo);
            model.put("verificationLink", appUrl + "/auth/verify/" + usuario.getVerificationToken());

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
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación inválido"));

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

        if (usuario.isEmailVerified()) {
            throw new IllegalArgumentException("El correo ya ha sido verificado");
        }

        String newToken = UUID.randomUUID().toString();
        usuario.setVerificationToken(newToken);
        usuarioRepository.save(usuario);

        sendVerificationEmail(usuario);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByEmail(loginDTO.getEmail())
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

        if (usuario.getApoderado() != null && usuario.getEmpleado() == null && usuario.getRoles().isEmpty()) {
            throw new BadCredentialsException("Los apoderados no tienen acceso al sistema");
        }

        List<String> userRoles = usuario.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        
        List<String> permissions = usuario.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());

        String jwt = tokenProvider.createToken(usuario.getEmail(), userRoles, companyId, permissions);
        String refreshToken = createRefreshToken(usuario);

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRefreshToken(refreshToken);
        response.setRoles(userRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setPermissions(permissions);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(
                usuario.getEmpleado() != null
                        ? Math.toIntExact(usuario.getEmpleado().getId())
                        : null
        );
        response.setPasswordChanged(usuario.isPasswordChanged());

        // Cargar menú dinámico basado en permisos
        Set<String> authorities = new HashSet<>(userRoles);
        authorities.addAll(permissions);
        response.setMenu(menuService.getMenuForUser(authorities));

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
    }

    @Override
    @Transactional
    public void changePassword(String email, veterinaria.vargasvet.dto.request.ChangePasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getOldPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setPasswordChanged(true);
        usuarioRepository.save(usuario);

        // Enviar notificación informativa
        sendPasswordChangeNotification(usuario);
    }

    @Override
    @Transactional
    public void resetPassword(veterinaria.vargasvet.dto.request.AdminChangePasswordRequest dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Aislamiento de datos: Si no es SUPER_ADMIN, solo puede resetear a usuarios de su misma empresa
        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (usuario.getCompany() == null || !usuario.getCompany().getId().equals(currentCompanyId)) {
                throw new org.springframework.security.access.AccessDeniedException("No tiene permisos para cambiar la contraseña de este usuario");
            }
        }

        usuario.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        usuario.setPasswordChanged(false);
        usuarioRepository.save(usuario);

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
    public AuthResponse refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh token no encontrado"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expirado. Por favor, inicie sesión nuevamente.");
        }

        Usuario usuario = refreshToken.getUsuario();
        
        List<String> userRoles = usuario.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        
        List<String> permissions = usuario.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());

        String newJwt = tokenProvider.createToken(usuario.getEmail(), userRoles, companyId, permissions);

        AuthResponse response = new AuthResponse();
        response.setToken(newJwt);
        response.setRefreshToken(token); // Mantener el mismo o rotar (aquí mantenemos por ahora)
        response.setRoles(userRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setPermissions(permissions);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());
        response.setEmpleadoId(usuario.getEmpleado() != null ? Math.toIntExact(usuario.getEmpleado().getId()) : null);
        
        Set<String> authorities = new HashSet<>(userRoles);
        authorities.addAll(permissions);
        response.setMenu(menuService.getMenuForUser(authorities));

        return response;
    }

    private String createRefreshToken(Usuario usuario) {
        String token = tokenProvider.createRefreshToken(usuario.getEmail());
        Instant expiryDate = Instant.now().plusSeconds(604800); // 7 días

        RefreshToken refreshToken = refreshTokenRepository.findByUsuario(usuario)
                .map(existing -> {
                    existing.setToken(token);
                    existing.setExpiryDate(expiryDate);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .usuario(usuario)
                        .token(token)
                        .expiryDate(expiryDate)
                        .build());

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String resolveNombreCompleto(Usuario usuario) {
        if (usuario.getNombre() != null) {
            return usuario.getNombre() + (usuario.getApellido() != null ? " " + usuario.getApellido() : "");
        }
        return usuario.getEmail();
    }

    private String resolveUserType(Usuario usuario) {
        if (usuario.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"))) return "SUPER_ADMIN";
        if (usuario.getEmpleado() != null) return "EMPLEADO";
        return "USUARIO";
    }
}
