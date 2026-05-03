package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.ERole;
import veterinaria.vargasvet.domain.entity.EmpleadoVeterinario;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.Genero;
import veterinaria.vargasvet.domain.enums.TipoDocumentoIdentidad;
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
        seedSuperAdmin();
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

    private void seedSuperAdmin() {
        String adminEmail = "administradorsystemsoft@gmail.com";
        if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setActivo(true);
            admin.setIsVerified(true);

            Role superAdminRole = roleRepository.findByName(ERole.ROLE_SUPER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role ROLE_SUPER_ADMIN not found."));
            admin.setRole(superAdminRole);

            EmpleadoVeterinario empleado = new EmpleadoVeterinario();
            empleado.setNombre("System");
            empleado.setApellido("Administrator");
            empleado.setTipoDocumentoIdentidad(TipoDocumentoIdentidad.DNI);
            empleado.setNumeroDocumentoIdentidad("00000000");
            empleado.setGenero(Genero.MASCULINO);
            empleado.setEstado(true);
            empleado.setUser(admin);

            admin.setEmpleadoVeterinario(empleado);

            usuarioRepository.save(admin);
            log.info("Super Admin user created: {}", adminEmail);
        }
    }
}
