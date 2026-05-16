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
        upsertRole("ROLE_SUPER_ADMIN", Arrays.asList(AppPermission.values()));

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
                AppPermission.CITA_READ, AppPermission.CITA_CREATE, AppPermission.CITA_UPDATE, AppPermission.CITA_CANCEL,
                AppPermission.SERVICIO_READ, AppPermission.SERVICIO_CREATE, AppPermission.SERVICIO_UPDATE, AppPermission.SERVICIO_DELETE, AppPermission.SERVICIO_TOGGLE,
                AppPermission.INV_READ, AppPermission.INV_WRITE,
                AppPermission.ADMIN_DASHBOARD, AppPermission.USER_MANAGE,
                AppPermission.HORARIO_READ, AppPermission.HORARIO_MANAGE
        ));

        upsertRole("ROLE_VETERINARIO", Arrays.asList(
                AppPermission.EMPLEADO_READ,
                AppPermission.ESPECIALIDAD_READ,
                AppPermission.SERVICIO_READ,
                AppPermission.APODERADO_READ, AppPermission.APODERADO_CREATE, AppPermission.APODERADO_UPDATE,
                AppPermission.PET_READ, AppPermission.PET_CREATE, AppPermission.PET_UPDATE, AppPermission.PET_STATUS,
                AppPermission.PET_HISTORY_READ, AppPermission.PET_HISTORY_WRITE,
                AppPermission.CLINICAL_RECORD_READ, AppPermission.CLINICAL_RECORD_MANAGE,
                AppPermission.CITA_READ, AppPermission.CITA_CREATE, AppPermission.CITA_UPDATE, AppPermission.CITA_CANCEL, AppPermission.CITA_INICIAR,
                AppPermission.HORARIO_READ, AppPermission.ADMIN_DASHBOARD, AppPermission.USER_READ
        ));

        upsertRole("ROLE_RECEPCIONISTA", Arrays.asList(
                AppPermission.EMPLEADO_READ,
                AppPermission.APODERADO_READ, AppPermission.APODERADO_CREATE, AppPermission.APODERADO_UPDATE,
                AppPermission.PET_READ, AppPermission.PET_CREATE, AppPermission.PET_UPDATE,
                AppPermission.PET_HISTORY_READ, AppPermission.CLINICAL_RECORD_READ,
                AppPermission.CITA_READ, AppPermission.CITA_CREATE, AppPermission.CITA_UPDATE, AppPermission.CITA_CANCEL,
                AppPermission.ADMIN_DASHBOARD, AppPermission.USER_READ
        ));

        upsertRole("ROLE_CLIENTE", java.util.Collections.emptyList());
    }

    private void upsertRole(String roleName, java.util.List<AppPermission> permissions) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setName(roleName);
            return r;
        });
        Set<Permission> rolePermissions = permissions.stream()
                .map(p -> permissionRepository.findByName(p.name())
                        .orElseThrow(() -> new IllegalStateException("Permission not found: " + p.name())))
                .collect(Collectors.toSet());
        role.setPermissions(rolePermissions);
        roleRepository.save(role);
        log.info("Role {} upserted with {} permissions.", roleName, permissions.size());
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

            // Horarios
            Menu horarios = createMenu("Gestión de Personal", "pi pi-users", null, 5, null, "EMPLEADO_READ");
            createMenu("Lista de Empleados", "pi pi-list", "/admin/empleados", 1, horarios, "EMPLEADO_READ");
            createMenu("Roster General", "pi pi-calendar", "/admin/empleados/horarios", 2, horarios, "HORARIO_READ");
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
