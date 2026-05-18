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
import veterinaria.vargasvet.domain.entity.Menu;
import veterinaria.vargasvet.repository.MenuRepository;


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
    private final MenuRepository menuRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing system data...");
        seedPermissions();
        seedRoles();
        seedMenus();
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
        createRoleIfNotFound("ROLE_SUPER_ADMIN", Arrays.asList(AppPermission.values()));

        createRoleIfNotFound("ROLE_ADMIN", Arrays.asList(
                AppPermission.USER_MANAGE,
                AppPermission.PET_READ, AppPermission.PET_WRITE,
                AppPermission.PET_HISTORY_READ, AppPermission.PET_HISTORY_WRITE,
                AppPermission.COMPANY_MANAGE, AppPermission.INV_READ, AppPermission.INV_WRITE
        ));

        createRoleIfNotFound("ROLE_VETERINARIO", Arrays.asList(
                AppPermission.CITA_READ, AppPermission.CITA_UPDATE,
                AppPermission.PET_READ, AppPermission.PET_HISTORY_READ,
                AppPermission.PET_HISTORY_WRITE
        ));

        createRoleIfNotFound("ROLE_RECEPCIONISTA", Arrays.asList(
                AppPermission.CITA_READ, AppPermission.CITA_CREATE, AppPermission.CITA_UPDATE,
                AppPermission.PET_READ, AppPermission.CLIENT_READ, AppPermission.CLIENT_WRITE
        ));
    }

    private void createRoleIfNotFound(String roleName, java.util.List<AppPermission> permissions) {
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

    private void seedMenus() {
        if (menuRepository.count() == 0) {
            log.info("Initializing menu items...");

            // Dashboard
            createMenu("Dashboard", "pi pi-home", "/dashboard", 1, null, null);

            // Citas
            Menu citas = createMenu("Citas", "pi pi-calendar", null, 2, null, "CITA_READ");
            createMenu("Agenda", "pi pi-calendar-plus", "/citas/agenda", 1, citas, "CITA_READ");

            // Pacientes
            Menu pacientes = createMenu("Mascotas", "pi pi-heart", null, 3, null, "PET_READ");
            createMenu("Lista de Mascotas", "pi pi-list", "/mascotas", 1, pacientes, "PET_READ");
            createMenu("Historias Clínicas", "pi pi-book", "/historias-clinicas", 2, pacientes, "PET_HISTORY_READ");

            // Administración
            Menu admin = createMenu("Administración", "pi pi-cog", null, 4, null, "USER_MANAGE");
            createMenu("Roles y Permisos", "pi pi-shield", "/admin/roles", 1, admin, "USER_MANAGE");
            createMenu("Gestión de Menús", "pi pi-list", "/admin/menus", 2, admin, "USER_MANAGE");
            createMenu("Usuarios", "pi pi-users", "/admin/usuarios", 3, admin, "USER_MANAGE");
            createMenu("Empresa", "pi pi-building", "/admin/empresa", 4, admin, "COMPANY_MANAGE");
        }
    }

    private Menu createMenu(String label, String icon, String path, int order, Menu parent, String permission) {
        Menu menu = new Menu();
        menu.setLabel(label);
        menu.setIcon(icon);
        menu.setPath(path);
        menu.setSortOrder(order);
        menu.setParent(parent);
        menu.setRequiredPermission(permission);
        return menuRepository.save(menu);
    }
}
