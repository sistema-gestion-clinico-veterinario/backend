package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Permission;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.enums.AppPermission;
import veterinaria.vargasvet.repository.PermissionRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import java.util.Arrays;
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
        Arrays.stream(AppPermission.values()).forEach(pEnum -> {
            permissionRepository.findByName(pEnum.name()).ifPresentOrElse(
                    existing -> {
                        // Actualizar metadatos si ya existe (idempotencia)
                        existing.setLabel(pEnum.getLabel());
                        existing.setDescription(pEnum.getDescription());
                        existing.setModule(pEnum.getModule());
                        permissionRepository.save(existing);
                    },
                    () -> {
                        // Crear nuevo si no existe
                        Permission permission = new Permission();
                        permission.setName(pEnum.name());
                        permission.setLabel(pEnum.getLabel());
                        permission.setDescription(pEnum.getDescription());
                        permission.setModule(pEnum.getModule());
                        permissionRepository.save(permission);
                        log.info("Permiso {} ({}) creado.", pEnum.name(), pEnum.getModule());
                    }
            );
        });
    }

    private void seedRoles() {
        upsertRole("ROLE_SUPER_ADMIN", Arrays.asList(AppPermission.values()));

        upsertRole("ROLE_APODERADO", Arrays.asList(
                AppPermission.APODERADO_DASHBOARD
        ));

        upsertRole("ROLE_ADMIN", Arrays.asList(
                AppPermission.USER_READ, AppPermission.USER_CREATE, AppPermission.USER_UPDATE,
                AppPermission.ROLE_MANAGE,
                AppPermission.COMPANY_READ, AppPermission.COMPANY_UPDATE, AppPermission.COMPANY_MANAGE,
                AppPermission.EMPLEADO_READ, AppPermission.EMPLEADO_CREATE, AppPermission.EMPLEADO_UPDATE, AppPermission.EMPLEADO_STATUS,
                AppPermission.TIPO_EMPLEADO_READ, AppPermission.TIPO_EMPLEADO_CREATE, AppPermission.TIPO_EMPLEADO_UPDATE, AppPermission.TIPO_EMPLEADO_STATUS,
                AppPermission.ESPECIALIDAD_READ, AppPermission.ESPECIALIDAD_CREATE, AppPermission.ESPECIALIDAD_UPDATE, AppPermission.ESPECIALIDAD_DELETE,
                AppPermission.APODERADO_READ, AppPermission.APODERADO_CREATE, AppPermission.APODERADO_UPDATE, AppPermission.APODERADO_STATUS,
                AppPermission.PET_READ, AppPermission.PET_CREATE, AppPermission.PET_UPDATE, AppPermission.PET_STATUS,
                AppPermission.PET_WRITE, AppPermission.PET_HISTORY_READ, AppPermission.PET_HISTORY_WRITE,
                AppPermission.CLINICAL_RECORD_READ, AppPermission.CLINICAL_RECORD_MANAGE,
                AppPermission.CITA_READ, AppPermission.CITA_CREATE, AppPermission.CITA_UPDATE, AppPermission.CITA_CANCEL, AppPermission.CITA_DELETE,
                AppPermission.SERVICIO_READ, AppPermission.SERVICIO_CREATE, AppPermission.SERVICIO_UPDATE, AppPermission.SERVICIO_DELETE, AppPermission.SERVICIO_TOGGLE,
                AppPermission.INV_READ, AppPermission.INV_WRITE,
                AppPermission.SALE_READ, AppPermission.SALE_MANAGE,
                AppPermission.ADMIN_DASHBOARD, AppPermission.EMPLEADO_DASHBOARD, AppPermission.USER_MANAGE,
                AppPermission.HORARIO_READ, AppPermission.HORARIO_MANAGE
        ));

    }

    private void upsertRole(String roleName, java.util.List<AppPermission> permissions) {
        Role role = roleRepository.findByName(roleName).orElse(null);

        if (role == null) {
            role = new Role();
            role.setName(roleName);
            Set<Permission> rolePermissions = permissions.stream()
                    .map(p -> permissionRepository.findByName(p.name())
                            .orElseThrow(() -> new IllegalStateException("Permission not found: " + p.name())))
                    .collect(Collectors.toSet());
            role.setPermissions(rolePermissions);
            roleRepository.save(role);
            log.info("Role {} created with {} initial permissions.", roleName, permissions.size());
        } else {
            // Role exists — add any missing permissions without removing existing ones
            Set<String> existingNames = role.getPermissions().stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
            Set<Permission> toAdd = permissions.stream()
                    .filter(p -> !existingNames.contains(p.name()))
                    .map(p -> permissionRepository.findByName(p.name())
                            .orElseThrow(() -> new IllegalStateException("Permission not found: " + p.name())))
                    .collect(Collectors.toSet());
            if (!toAdd.isEmpty()) {
                role.getPermissions().addAll(toAdd);
                roleRepository.save(role);
                log.info("Role {} updated: added {} missing permissions.", roleName, toAdd.size());
            } else {
                log.info("Role {} already up-to-date.", roleName);
            }
        }
    }

}
