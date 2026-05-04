package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.enums.EPermission;
import veterinaria.vargasvet.repository.PermissionRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing system data...");
        seedPermissions();
        seedRoles();
        log.info("System data initialization completed.");
    }

    private void seedPermissions() {
        Arrays.stream(EPermission.values()).forEach(ePerm -> {
            if (permissionRepository.findByName(ePerm.name()).isEmpty()) {
                Permission permission = new Permission();
                permission.setName(ePerm.name());
                permissionRepository.save(permission);
                log.info("Permission {} created.", ePerm.name());
            }
        });
    }

    private void seedRoles() {
        createRoleIfNotFound("ROLE_SUPER_ADMIN", Arrays.asList(EPermission.values()));
        createRoleIfNotFound("ROLE_ADMIN", Arrays.asList(
                EPermission.USER_READ, EPermission.USER_CREATE, EPermission.USER_UPDATE,
                EPermission.PET_READ, EPermission.PET_CREATE, EPermission.PET_UPDATE,
                EPermission.CLINICAL_RECORD_READ, EPermission.CLINICAL_RECORD_MANAGE,
                EPermission.COMPANY_MANAGE, EPermission.INVENTORY_READ, EPermission.INVENTORY_MANAGE
        ));
        createRoleIfNotFound("ROLE_VETERINARIO", Arrays.asList(
                EPermission.PET_READ, EPermission.CLINICAL_RECORD_READ, 
                EPermission.CLINICAL_RECORD_MANAGE, EPermission.VETERINARY_PRACTICE
        ));
        createRoleIfNotFound("ROLE_RECEPCIONISTA", Arrays.asList(
                EPermission.PET_READ, EPermission.PET_CREATE, EPermission.USER_READ
        ));
    }

    private void createRoleIfNotFound(String roleName, java.util.List<EPermission> permissions) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            Set<Permission> rolePermissions = permissions.stream()
                    .map(p -> permissionRepository.findByName(p.name()).get())
                    .collect(Collectors.toSet());
            role.setPermissions(rolePermissions);
            roleRepository.save(role);
            log.info("Role {} created with {} permissions.", roleName, permissions.size());
        }
    }
}
