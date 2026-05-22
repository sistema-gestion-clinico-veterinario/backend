package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.RolVentanaPermiso;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.repository.RolVentanaPermisoRepository;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.VistaRepository;

import java.util.List;
import java.util.Set;

@Component
@Order(1) // Asegura que se ejecute antes que otros componentes
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVentanaPermisoRepository rolVentanaPermisoRepository;
    private final VistaRepository vistaRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== INICIALIZANDO DATOS DEL SISTEMA ===");

        // SOLO crear roles si no existen - NUNCA actualizar
        seedRolesIfNotExist();

        // SOLO crear ventanas si no existen - NUNCA actualizar
        seedVentanasIfNotExist();

        // SOLO crear vistas si no existen
        seedVistasIfNotExist();

        // SOLO crear permisos SUPER_ADMIN si no existen
        seedSuperAdminPermisosIfNotExist();

        log.info("=== INICIALIZACIÓN COMPLETADA ===");
        log.info("NOTA: Las ventanas y roles existentes NO fueron modificados");
    }

    // ─────────────────────────────────────────────
    // ROLES - SOLO CREAR, NUNCA ACTUALIZAR
    // ─────────────────────────────────────────────

    private void seedRolesIfNotExist() {
        createRoleIfNotExists("ROLE_SUPER_ADMIN", "Administrador global del sistema");
        createRoleIfNotExists("ROLE_ADMIN", "Administrador de empresa");
        createRoleIfNotExists("ROLE_EMPLEADO", "Empleado / Veterinario");
        createRoleIfNotExists("ROLE_CLIENTE", "Cliente / Apoderado");
    }

    private void createRoleIfNotExists(String nombre, String descripcion) {
        if (!roleRepository.existsByName(nombre)) {
            Role role = new Role();
            role.setName(nombre);
            role.setDescripcion(descripcion);
            roleRepository.save(role);
            log.info("✅ Rol creado: {}", nombre);
        } else {
            log.debug("⏭️ Rol ya existe, no se modifica: {}", nombre);
        }
    }

    // ─────────────────────────────────────────────
    // VENTANAS - SOLO CREAR, NUNCA ACTUALIZAR
    // ─────────────────────────────────────────────

    private void seedVentanasIfNotExist() {

        // ══════════════════════════════════════════
        // VENTANAS RAÍCES (menú principal)
        // ══════════════════════════════════════════
        createVentanaIfNotExists("DASHBOARD", "Dashboard", "pi pi-home", "admin/dashboard", null, 1);
        createVentanaIfNotExists("ADMIN", "Administración", "pi pi-cog", null, null, 2);
        createVentanaIfNotExists("ATENCION", "Atención Clínica", "pi pi-heart", null, null, 3);
        createVentanaIfNotExists("CLIENTES", "Clientes", "pi pi-users", null, null, 4);
        createVentanaIfNotExists("EMPLEADO", "Mi Espacio", "pi pi-user-edit", null, null, 5);
        createVentanaIfNotExists("PORTAL", "Mi Portal", "pi pi-user", null, null, 6, false); // desactivado: los ítems del portal son raíces directas
        createVentanaIfNotExists("PERFIL", "Mi Perfil", "pi pi-user", "profile", null, 7);
        createVentanaIfNotExists("CAMBIAR_CONTRASENA", "Cambiar Contraseña", "pi pi-key", "password-change", null, 8);

        // ══════════════════════════════════════════
        // HIJOS DE ADMIN
        // ══════════════════════════════════════════
        Ventana admin = ventanaRepository.findByCodigo("ADMIN").orElse(null);
        createVentanaIfNotExists("EMPLEADOS", "Empleados", "pi pi-id-card", "empleados", admin, 1);
        createVentanaIfNotExists("ROLES", "Roles y Permisos", "pi pi-shield", "roles", admin, 2);
        createVentanaIfNotExists("EMPRESA", "Mi Empresa", "pi pi-building", "company", admin, 3);
        createVentanaIfNotExists("AUDITORIA", "Auditoría", "pi pi-list", "auditoria", admin, 4);
        createVentanaIfNotExists("REPORTES", "Reportes", "pi pi-chart-bar", "reportes", admin, 5, false);
        createVentanaIfNotExists("HORARIOS", "Gestión de Horarios", "pi pi-calendar", "empleados/horarios", admin, 6);
        createVentanaIfNotExists("COMPLEMENTARIO", "Datos Complementarios", "pi pi-database", "complementario", admin, 7);

        // ══════════════════════════════════════════
        // HIJOS DE ATENCIÓN CLÍNICA
        // ══════════════════════════════════════════
        Ventana atencion = ventanaRepository.findByCodigo("ATENCION").orElse(null);
        createVentanaIfNotExists("CITAS", "Citas", "pi pi-calendar", "citas/agenda", atencion, 1);
        createVentanaIfNotExists("MASCOTAS", "Mascotas", "pi pi-heart-fill", "mascotas", atencion, 2);
        createVentanaIfNotExists("HISTORIAS", "Historias Clínicas", "pi pi-folder", "historias-clinicas", atencion, 3);
        createVentanaIfNotExists("RECETAS", "Recetas", "pi pi-file", "recetas", atencion, 4);
        createVentanaIfNotExists("SERVICIOS", "Servicios", "pi pi-tag", "servicios", atencion, 5, false);

        // ══════════════════════════════════════════
        // HIJOS DE CLIENTES (gestión de apoderados)
        // ══════════════════════════════════════════
        Ventana clientes = ventanaRepository.findByCodigo("CLIENTES").orElse(null);
        createVentanaIfNotExists("APODERADOS", "Apoderados", "pi pi-users", "clientes", clientes, 1);
        createVentanaIfNotExists("FACTURACION", "Facturación", "pi pi-dollar", "pagos", clientes, 2);
        createVentanaIfNotExists("INVENTARIO", "Inventario", "pi pi-box", "inventario", clientes, 3, false);

        // ══════════════════════════════════════════
        // HIJOS DE MI ESPACIO (EMPLEADO)
        // ══════════════════════════════════════════
        Ventana empleado = ventanaRepository.findByCodigo("EMPLEADO").orElse(null);
        createVentanaIfNotExists("EMPLEADO_DASHBOARD", "Dashboard", "pi pi-home", "empleado/dashboard", empleado, 1);
        createVentanaIfNotExists("MI_HORARIO", "Mi Horario", "pi pi-calendar", "mi-horario", empleado, 2);

        // ══════════════════════════════════════════
        // PORTAL DEL APODERADO — ítems raíz directos
        // (sin padre, aparecen como ítems planos en el menú)
        // El admin puede reagruparlos desde Gestión de Menús
        // ══════════════════════════════════════════
        createVentanaIfNotExists("APODERADO_DASHBOARD", "Mi Portal",    "pi pi-chart-line",  "apoderado/dashboard",    null, 1);
        createVentanaIfNotExists("MIS_MASCOTAS",        "Mis Mascotas", "pi pi-heart",       "apoderado/mis-mascotas", null, 2);
        createVentanaIfNotExists("MIS_CITAS",           "Mis Citas",    "pi pi-calendar",    "apoderado/mis-citas",    null, 3);
        createVentanaIfNotExists("MIS_PAGOS",           "Mis Pagos",    "pi pi-wallet",      "apoderado/mis-pagos",    null, 4);
        createVentanaIfNotExists("MIS_HISTORIAL",       "Mi Historial", "pi pi-folder-open", "apoderado/mi-historial", null, 5);
    }

    // ─────────────────────────────────────────────
    // PERMISOS SUPER ADMIN - SOLO SI NO EXISTEN
    // ─────────────────────────────────────────────

    private void seedSuperAdminPermisosIfNotExist() {
        List<Role> roles = roleRepository.findAllByName("ROLE_SUPER_ADMIN");
        if (roles.isEmpty()) {
            log.warn("⚠️ ROLE_SUPER_ADMIN no encontrado");
            return;
        }
        Role superAdmin = roles.get(0);

        // Ventanas que el SUPER_ADMIN NO debe tener (son exclusivas de otros roles)
        // PORTAL no necesita excluirse: ya tiene activo=false en la BD
        Set<String> excluidas = Set.of(
                "APODERADO_DASHBOARD", "MIS_MASCOTAS", "MIS_CITAS",
                "MIS_PAGOS", "MIS_HISTORIAL",          // portal del apoderado
                "EMPLEADO", "EMPLEADO_DASHBOARD", "MI_HORARIO",  // espacio del empleado
                "REPORTES", "INVENTARIO", "SERVICIOS"  // no implementados
        );

        List<Ventana> todas = ventanaRepository.findAll();
        int creados = 0;

        for (Ventana v : todas) {
            if (excluidas.contains(v.getCodigo())) continue;

            // SOLO crear si NO existe el permiso
            if (!rolVentanaPermisoRepository.existsByRolIdAndVentanaId(superAdmin.getId(), v.getId())) {
                RolVentanaPermiso p = new RolVentanaPermiso();
                p.setRol(superAdmin);
                p.setVentana(v);
                p.setLeer(true);
                p.setEscribir(true);
                p.setModificar(true);
                p.setEliminar(true);
                rolVentanaPermisoRepository.save(p);
                creados++;
                log.debug("Permiso SUPER_ADMIN creado para: {}", v.getCodigo());
            }
        }

        if (creados > 0) {
            log.info("✅ Creados {} permisos para SUPER_ADMIN", creados);
        } else {
            log.debug("⏭️ Todos los permisos de SUPER_ADMIN ya existían");
        }
    }

    // ─────────────────────────────────────────────
    // VISTAS - SOLO CREAR SI NO EXISTEN
    // ─────────────────────────────────────────────

    private void seedVistasIfNotExist() {
        // Find Windows by code and seed their views
        seedVistaIfNotExists("VISTA_DASHBOARD_ADMIN", "Dashboard Administrador", "admin/dashboard", "DASHBOARD");
        seedVistaIfNotExists("VISTA_DASHBOARD", "Dashboard General", "dashboard", "DASHBOARD");

        seedVistaIfNotExists("VISTA_COMPANY_ADMIN", "Mi Empresa Admin", "admin/company", "EMPRESA");
        seedVistaIfNotExists("VISTA_COMPANY", "Mi Empresa General", "company", "EMPRESA");

        seedVistaIfNotExists("VISTA_AUDITORIA_ADMIN", "Auditoría Admin", "admin/auditoria", "AUDITORIA");
        seedVistaIfNotExists("VISTA_AUDITORIA", "Auditoría General", "auditoria", "AUDITORIA");

        seedVistaIfNotExists("VISTA_ROLES_ADMIN", "Roles Admin", "admin/roles", "ROLES");
        seedVistaIfNotExists("VISTA_ROLES", "Roles General", "roles", "ROLES");

        seedVistaIfNotExists("VISTA_COMPLEMENTARIO_ADMIN", "Complementario Admin", "admin/complementario", "COMPLEMENTARIO");
        seedVistaIfNotExists("VISTA_COMPLEMENTARIO", "Complementario General", "complementario", "COMPLEMENTARIO");

        seedVistaIfNotExists("VISTA_EMPLEADOS_ADMIN", "Empleados Admin", "admin/empleados", "EMPLEADOS");
        seedVistaIfNotExists("VISTA_EMPLEADOS", "Empleados General", "empleados", "EMPLEADOS");

        seedVistaIfNotExists("VISTA_APODERADOS_ADMIN", "Apoderados Admin", "admin/clientes", "APODERADOS");
        seedVistaIfNotExists("VISTA_APODERADOS", "Apoderados General", "clientes", "APODERADOS");

        seedVistaIfNotExists("VISTA_MASCOTAS", "Mascotas General", "mascotas", "MASCOTAS");
        seedVistaIfNotExists("VISTA_MASCOTAS_ADMIN", "Mascotas Admin", "admin/mascotas", "MASCOTAS");
        seedVistaIfNotExists("VISTA_MASCOTAS_EMPLEADO", "Mascotas Empleado", "empleado/mascotas", "MASCOTAS");

        seedVistaIfNotExists("VISTA_RECETAS", "Recetas General", "recetas", "RECETAS");
        seedVistaIfNotExists("VISTA_RECETAS_ADMIN", "Recetas Admin", "admin/recetas", "RECETAS");
        seedVistaIfNotExists("VISTA_RECETAS_EMPLEADO", "Recetas Empleado", "empleado/recetas", "RECETAS");

        seedVistaIfNotExists("VISTA_HISTORIAS", "Historias Clínicas General", "historias-clinicas", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_MASCOTA", "Historia Mascota", "historias-clinicas/mascota/:mascotaId", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_CONSULTA", "Historia Consulta", "historias-clinicas/consulta/:consultaId", "HISTORIAS");
        
        seedVistaIfNotExists("VISTA_HISTORIAS_ADMIN", "Historias Clínicas Admin", "admin/historias-clinicas", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_MASCOTA_ADMIN", "Historia Mascota Admin", "admin/historias-clinicas/mascota/:mascotaId", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_CONSULTA_ADMIN", "Historia Consulta Admin", "admin/historias-clinicas/consulta/:consultaId", "HISTORIAS");
        
        seedVistaIfNotExists("VISTA_HISTORIAS_EMPLEADO", "Historias Clínicas Empleado", "empleado/historias-clinicas", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_MASCOTA_EMPLEADO", "Historia Mascota Empleado", "empleado/historias-clinicas/mascota/:mascotaId", "HISTORIAS");
        seedVistaIfNotExists("VISTA_HISTORIAS_CONSULTA_EMPLEADO", "Historia Consulta Empleado", "empleado/historias-clinicas/consulta/:consultaId", "HISTORIAS");

        seedVistaIfNotExists("VISTA_CITAS", "Citas Agenda General", "citas/agenda", "CITAS");
        seedVistaIfNotExists("VISTA_CITAS_ADMIN", "Citas Agenda Admin", "admin/citas/agenda", "CITAS");
        seedVistaIfNotExists("VISTA_CITAS_EMPLEADO", "Citas Agenda Empleado", "empleado/citas/agenda", "CITAS");

        seedVistaIfNotExists("VISTA_HORARIOS_ADMIN", "Horarios Roster Admin", "admin/empleados/horarios", "HORARIOS");
        seedVistaIfNotExists("VISTA_HORARIOS", "Horarios Roster General", "empleados/horarios", "HORARIOS");
        seedVistaIfNotExists("VISTA_HORARIO_DETALLE_ADMIN", "Horario Detalle Admin", "admin/empleados/:id/horario", "HORARIOS");
        seedVistaIfNotExists("VISTA_HORARIO_DETALLE", "Horario Detalle General", "empleados/:id/horario", "HORARIOS");

        seedVistaIfNotExists("VISTA_MI_HORARIO_EMPLEADO", "Mi Horario Empleado", "empleado/mi-horario", "MI_HORARIO");
        seedVistaIfNotExists("VISTA_MI_HORARIO", "Mi Horario General", "mi-horario", "MI_HORARIO");

        seedVistaIfNotExists("VISTA_EMPLEADO_DASHBOARD", "Empleado Dashboard", "empleado/dashboard", "EMPLEADO_DASHBOARD");

        seedVistaIfNotExists("VISTA_PROFILE", "Perfil", "profile", "PERFIL");
        seedVistaIfNotExists("VISTA_PASSWORD_CHANGE", "Cambiar Contraseña", "password-change", "CAMBIAR_CONTRASENA");

        seedVistaIfNotExists("VISTA_APODERADO_DASHBOARD", "Apoderado Dashboard", "apoderado/dashboard", "APODERADO_DASHBOARD");
        seedVistaIfNotExists("VISTA_APODERADO", "Apoderado Portal", "apoderado", "APODERADO_DASHBOARD");

        seedVistaIfNotExists("VISTA_MIS_CITAS", "Mis Citas General", "mis-citas", "MIS_CITAS");
        seedVistaIfNotExists("VISTA_MIS_CITAS_APODERADO", "Mis Citas Apoderado", "apoderado/mis-citas", "MIS_CITAS");

        seedVistaIfNotExists("VISTA_MIS_MASCOTAS", "Mis Mascotas General", "mis-mascotas", "MIS_MASCOTAS");
        seedVistaIfNotExists("VISTA_MIS_MASCOTAS_APODERADO", "Mis Mascotas Apoderado", "apoderado/mis-mascotas", "MIS_MASCOTAS");

        seedVistaIfNotExists("VISTA_APODERADO_MI_HISTORIAL", "Mi Historial Apoderado", "apoderado/mi-historial", "MIS_HISTORIAL");
        seedVistaIfNotExists("VISTA_MI_HISTORIAL", "Mi Historial Mascota General", "mi-historial/:mascotaId", "MIS_HISTORIAL");

        seedVistaIfNotExists("VISTA_MIS_PAGOS", "Mis Pagos General", "mis-pagos", "MIS_PAGOS");
        seedVistaIfNotExists("VISTA_MIS_PAGOS_APODERADO", "Mis Pagos Apoderado", "apoderado/mis-pagos", "MIS_PAGOS");

        seedVistaIfNotExists("VISTA_FACTURACION_ADMIN", "Historial Pagos Admin", "admin/pagos", "FACTURACION");
        seedVistaIfNotExists("VISTA_FACTURACION", "Historial Pagos General", "pagos", "FACTURACION");
    }

    private void seedVistaIfNotExists(String codigo, String nombre, String ruta, String codigoVentana) {
        if (!vistaRepository.existsByCodigo(codigo)) {
            Ventana v = ventanaRepository.findByCodigo(codigoVentana).orElse(null);
            if (v == null) {
                log.warn("⚠️ Ventana con código {} no encontrada. No se puede asociar la vista {}", codigoVentana, codigo);
                return;
            }
            Vista vista = new Vista();
            vista.setCodigo(codigo);
            vista.setNombre(nombre);
            vista.setRuta(ruta);
            vista.setVentana(v);
            vista.setActivo(true);
            vistaRepository.save(vista);
            log.info("✅ Vista creada: {} ({})", codigo, nombre);
        } else {
            log.debug("⏭️ Vista ya existe, no se modifica: {}", codigo);
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS - SOLO CREAR SI NO EXISTE
    // ─────────────────────────────────────────────

    private void createVentanaIfNotExists(String codigo, String nombre, String icono,
                                          String ruta, Ventana parent, int orden) {
        createVentanaIfNotExists(codigo, nombre, icono, ruta, parent, orden, true);
    }

    private void createVentanaIfNotExists(String codigo, String nombre, String icono,
                                          String ruta, Ventana parent, int orden, boolean activo) {
        if (!ventanaRepository.existsByCodigo(codigo)) {
            Ventana v = new Ventana();
            v.setCodigo(codigo);
            v.setNombre(nombre);
            v.setIcono(icono);
            v.setRuta(ruta);
            v.setParent(parent);
            v.setOrden(orden);
            v.setActivo(activo);
            ventanaRepository.save(v);
            log.info("✅ Ventana creada: {} ({})", codigo, nombre);
        } else {
            log.debug("⏭️ Ventana ya existe, no se modifica: {}", codigo);
        }
    }
}