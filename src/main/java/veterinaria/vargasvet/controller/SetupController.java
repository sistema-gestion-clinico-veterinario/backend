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
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.request.UserRegistrationDTO;
import veterinaria.vargasvet.dto.response.UserProfileDTO;
import veterinaria.vargasvet.mapper.UserMapper;
import veterinaria.vargasvet.domain.entity.UsuarioPorRol;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @PostMapping("/first-admin")
    public ResponseEntity<?> createFirstAdmin(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        if (usuarioRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("El sistema ya ha sido inicializado. No se pueden crear más administradores base.");
        }
        Usuario admin = userMapper.toEntity(registrationDTO);
        admin.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        admin.setActivo(true);
        admin.setEmailVerified(true);
        admin.setPasswordChanged(true);

        Usuario saved = usuarioRepository.save(admin);

        roleRepository.findByName("ROLE_SUPER_ADMIN").ifPresent(role -> {
            UsuarioPorRol upr = new UsuarioPorRol();
            upr.setUsuario(saved);
            upr.setRol(role);
            usuarioPorRolRepository.save(upr);
        });
        UserProfileDTO response = userMapper.toProfileDTO(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
