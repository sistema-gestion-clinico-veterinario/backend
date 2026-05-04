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
import veterinaria.vargasvet.security.TokenProvider;

import org.springframework.beans.factory.annotation.Value;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.service.EmailService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements veterinaria.vargasvet.service.UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final EmailService emailService;

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

        AuthResponse response = new AuthResponse();
        response.setToken(jwt);
        response.setRoles(userRoles);
        response.setCompanyId(companyId);
        response.setCompanyName(usuario.getCompany() != null ? usuario.getCompany().getName() : null);
        response.setPermissions(permissions);
        response.setNombreCompleto(resolveNombreCompleto(usuario));
        response.setUserType(resolveUserType(usuario));
        response.setPasswordChanged(usuario.isPasswordChanged());

        return response;
    }

    @Override
    public UserProfileDTO getProfile(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return userMapper.toProfileDTO(usuario);
    }

    @Override
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
