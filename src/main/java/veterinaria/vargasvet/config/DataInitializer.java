package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.enums.ERole;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing system data...");
        seedRoles();
        log.info("System data initialization completed.");
    }

    private void seedRoles() {
        Arrays.stream(ERole.values()).forEach(eRole -> {
            if (roleRepository.findByName(eRole).isEmpty()) {
                Role role = new Role();
                role.setName(eRole);
                roleRepository.save(role);
                log.info("Role {} created.", eRole);
            }
        });
    }
}
