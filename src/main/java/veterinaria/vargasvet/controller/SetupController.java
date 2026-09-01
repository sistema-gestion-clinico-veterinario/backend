package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.domain.enums.RolePurpose;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.setup.enabled", havingValue = "true")
public class SetupController {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final veterinaria.vargasvet.security.PasswordPolicyService passwordPolicyService;

    @Value("${app.setup.token:}")
    private String setupToken;

    @PostMapping("/first-admin")
    @Transactional
    public ResponseEntity<?> createFirstAdmin(
            @RequestHeader(value = "X-Setup-Token", required = false) String providedToken,
            @Valid @RequestBody UserRegistrationDTO registrationDTO) {
        if (setupToken.isBlank() || providedToken == null || !java.security.MessageDigest.isEqual(
                setupToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                providedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(?)", 0x53595354454D5645L);
        if (usuarioRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El sistema ya ha sido inicializado. No se pueden crear más administradores base.");
        }
        passwordPolicyService.validate(registrationDTO.getPassword(), registrationDTO.getEmail(),
                registrationDTO.getNombre(), registrationDTO.getApellido());
        Usuario admin = userMapper.toEntity(registrationDTO);
        admin.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        admin.setActivo(true);
        admin.setEmailVerified(true);
        admin.setPasswordChanged(true);

        Usuario saved = usuarioRepository.save(admin);

        roleRepository.findFirstByCompanyIsNullAndPurpose(RolePurpose.PLATFORM_ADMIN).ifPresent(role -> {
            UsuarioPorRol upr = new UsuarioPorRol();
            upr.setUsuario(saved);
            upr.setRol(role);
            usuarioPorRolRepository.save(upr);
        });
        UserProfileDTO response = userMapper.toProfileDTO(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
