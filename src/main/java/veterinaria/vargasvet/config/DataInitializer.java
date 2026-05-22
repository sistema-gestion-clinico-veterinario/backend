package veterinaria.vargasvet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Role;
import veterinaria.vargasvet.domain.entity.Vista;
import veterinaria.vargasvet.domain.entity.Ventana;
import veterinaria.vargasvet.domain.entity.RolVistaPermiso;
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.VistaRepository;
import veterinaria.vargasvet.repository.VentanaRepository;
import veterinaria.vargasvet.repository.RolVistaPermisoRepository;
import java.util.List;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final VistaRepository vistaRepository;
    private final VentanaRepository ventanaRepository;
    private final RolVistaPermisoRepository rolVistaPermisoRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== INICIALIZANDO DATOS DEL SISTEMA ===");

        seedRolesIfNotExist();
        seedVentanasIfNotExist();
        seedVistasIfNotExist();
        deactivateObsoleteVentanas();
        deactivateObsoleteVistas();
        seedPermisosIfNotExist();

        log.info("=== INICIALIZACIÓN COMPLETADA ===");
    }

    private void seedRolesIfNotExist() {
        createRoleIfNotExists("ROLE_SUPER_ADMIN", "Administrador global del sistema");
        createRoleIfNotExists("ROLE_ADMIN",       "Administrador de empresa");
    }

    private void createRoleIfNotExists(String nombre, String descripcion) {
        if (!roleRepository.existsByName(nombre)) {
            Role role = new Role();
            role.setName(nombre);
            role.setDescripcion(descripcion);
            roleRepository.save(role);
            log.info(" Rol creado: {}", nombre);
        } else {
            log.debug("⏭ Rol ya existe: {}", nombre);
        }
    }

    private void seedVentanasIfNotExist() {
        log.info("Inicializando ventanas...");
        createVentanaIfNotExists("ADMINISTRACION",   "Administración",   "administracion", 2);
        createVentanaIfNotExists("PERSONAL",          "Personal",         "rrhh",           3);
        createVentanaIfNotExists("CLINICA",           "Clínica",          "clinica",         4);
        createVentanaIfNotExists("PORTAL_APODERADO",  "Portal Apoderado", "apoderado",       5);
    }

    private void createVentanaIfNotExists(String codigo, String nombre, String grupo, Integer orden) {
        Ventana ventana = ventanaRepository.findByCodigo(codigo);
        boolean esNueva = ventana == null;

        if (esNueva) {
            ventana = new Ventana();
            ventana.setCodigo(codigo);
        }

        ventana.setNombre(nombre);
        ventana.setGrupo(grupo);
        ventana.setOrden(orden);
        ventana.setActivo(true);
        ventanaRepository.save(ventana);

        if (esNueva) {
            log.info(" Ventana creada: {}", codigo);
        }
    }

    private void seedVistasIfNotExist() {
        Ventana administracion  = ventanaRepository.findByCodigo("ADMINISTRACION");
        Ventana personal        = ventanaRepository.findByCodigo("PERSONAL");
        Ventana clinica         = ventanaRepository.findByCodigo("CLINICA");
        Ventana portalApoderado = ventanaRepository.findByCodigo("PORTAL_APODERADO");

        seed("VISTA_DASHBOARD",          "Dashboard",          "/dashboard",          "GENERAL", 1,  null, null);
        seed("VISTA_EMPLEADO_DASHBOARD", "Dashboard Empleado", "/empleado/dashboard", "GENERAL", 2,  null, null);
        seed("VISTA_PROFILE",            "Mi Perfil",          "/profile",            "GENERAL", 98, null, null);

        seed("VISTA_COMPANY",         "Mi Empresa",        "/company",        "ADMIN", 1, administracion, null);
        seed("VISTA_AUDITORIA_ADMIN", "Auditoría",         "/auditoria",      "ADMIN", 2, administracion, null);
        seed("VISTA_ROLES",           "Roles",             "/roles",          "ADMIN", 3, administracion, null);
        seed("VISTA_VENTANAS",        "Gestión de Vistas", "/ventanas",       "ADMIN", 4, administracion, null);
        seed("VISTA_COMPLEMENTARIO",  "Complementario",    "/complementario", "ADMIN", 5, administracion, null);
        seed("VISTA_PAGOS",           "Pagos",             "/pagos",          "ADMIN", 6, administracion, null);

        seed("VISTA_EMPLEADOS",  "Empleados",  "/empleados",          "RRHH", 1, personal, null);
        seed("VISTA_HORARIOS",   "Horarios",   "/empleados/horarios", "RRHH", 2, personal, null);
        seed("VISTA_MI_HORARIO", "Mi Horario", "/mi-horario",         "RRHH", 3, personal, null);

        seed("VISTA_CLIENTES",     "Clientes",           "/clientes",           "CLINICA", 1, clinica, null);
        seed("VISTA_MASCOTAS",     "Mascotas",           "/mascotas",           "CLINICA", 2, clinica, null);
        seed("VISTA_RECETAS",      "Recetas",            "/recetas",            "CLINICA", 3, clinica, null);
        seed("VISTA_HISTORIAS",    "Historias Clínicas", "/historias-clinicas", "CLINICA", 4, clinica, null);
        seed("VISTA_CITAS_AGENDA", "Agenda de Citas",    "/citas/agenda",       "CLINICA", 5, clinica, null);

        seed("VISTA_APODERADO_DASHBOARD", "Mi Portal",    "/apoderado/dashboard",    "APODERADO", 1, portalApoderado, null);
        seed("VISTA_MIS_MASCOTAS",        "Mis Mascotas", "/apoderado/mis-mascotas", "APODERADO", 2, portalApoderado, null);
        seed("VISTA_MIS_PAGOS",           "Mis Pagos",    "/apoderado/mis-pagos",    "APODERADO", 5, portalApoderado, null);

        Vista misMascotas = vistaRepository.findByCodigo("VISTA_MIS_MASCOTAS").orElse(null);

        seed("VISTA_MIS_CITAS",     "Mis Citas",     "/apoderado/mis-citas",    "APODERADO", 1, portalApoderado, misMascotas);
        seed("VISTA_MI_HISTORIAL",  "Mi Historial",  "/apoderado/mi-historial", "APODERADO", 2, portalApoderado, misMascotas);
        seed("VISTA_MIS_RECETAS",   "Mis Recetas",   "/apoderado/mis-recetas",  "APODERADO", 3, portalApoderado, misMascotas);
    }

    private void seed(String codigo, String nombre, String ruta, String grupo,
                      Integer orden, Ventana ventana, Vista parent) {
        Vista vista = vistaRepository.findByCodigo(codigo).orElseGet(Vista::new);
        boolean esNueva = vista.getId() == null;

        vista.setCodigo(codigo);
        vista.setNombre(nombre);
        vista.setRuta(ruta);
        vista.setGrupo(grupo);
        vista.setOrden(orden);
        vista.setVentana(ventana);
        vista.setActivo(true);

        if (esNueva) {
            vista.setParent(parent);
        }

        vistaRepository.save(vista);

        if (esNueva) {
            log.info("Vista creada: {} → {}", codigo, ruta);
        } else {
            log.debug("Vista actualizada: {}", codigo);
        }
    }
    private void seedPermisosIfNotExist() {
        log.info("Verificando permisos iniciales del SUPER_ADMIN...");
        Role superAdmin = roleRepository.findByName("ROLE_SUPER_ADMIN").orElse(null);

        if (superAdmin == null) {
            log.warn("️ ROLE_SUPER_ADMIN no encontrado");
            return;
        }

        List<RolVistaPermiso> permisosExistentes = rolVistaPermisoRepository.findByRolId(superAdmin.getId());

        if (!permisosExistentes.isEmpty()) {
            log.info("⏭ SUPER_ADMIN ya tiene {} permisos configurados, no se sobreescribe", permisosExistentes.size());
            return;
        }

        List<Vista> vistas = vistaRepository.findByActivoTrue();
        int contadorCreados = 0;

        for (Vista vista : vistas) {
            RolVistaPermiso permiso = new RolVistaPermiso();
            permiso.setRol(superAdmin);
            permiso.setVista(vista);
            permiso.setLeer(true);
            permiso.setEscribir(true);
            permiso.setModificar(true);
            permiso.setEliminar(true);
            rolVistaPermisoRepository.save(permiso);
            contadorCreados++;
        }

        log.info(" {} permisos iniciales asignados al SUPER_ADMIN", contadorCreados);
    }

    private void seed(String codigo, String nombre, String ruta, String grupo, Integer orden, Ventana ventana) {
        Vista vista = vistaRepository.findByCodigo(codigo).orElseGet(Vista::new);
        boolean esNueva = vista.getId() == null;

        vista.setCodigo(codigo);
        vista.setNombre(nombre);
        vista.setRuta(ruta);
        vista.setGrupo(grupo);
        vista.setOrden(orden);
        vista.setVentana(ventana);
        vista.setActivo(true);
        vistaRepository.save(vista);

        if (esNueva) {
            log.info(" Vista creada: {} → {}", codigo, ruta);
        } else {
            log.debug("⏭ Vista actualizada: {}", codigo);
        }
    }

    private void deactivateObsoleteVistas() {
        Set<String> codigosObsoletos = Set.of(
                "VISTA_AGENDA",
                "VISTA_DASHBOARD_ADMIN",
                "VISTA_COMPANY_ADMIN",
                "VISTA_AUDITORIA",
                "VISTA_ROLES_ADMIN",
                "VISTA_COMPLEMENTARIO_ADMIN",
                "VISTA_EMPLEADOS_ADMIN",
                "VISTA_APODERADOS_ADMIN",
                "VISTA_APODERADOS",
                "VISTA_MASCOTAS_ADMIN",
                "VISTA_MASCOTAS_EMPLEADO",
                "VISTA_RECETAS_ADMIN",
                "VISTA_RECETAS_EMPLEADO",
                "VISTA_HISTORIAS_ADMIN",
                "VISTA_HISTORIAS_EMPLEADO",
                "VISTA_HISTORIAS_MASCOTA",
                "VISTA_HISTORIAS_CONSULTA",
                "VISTA_HISTORIAS_MASCOTA_ADMIN",
                "VISTA_HISTORIAS_CONSULTA_ADMIN",
                "VISTA_HISTORIAS_MASCOTA_EMPLEADO",
                "VISTA_HISTORIAS_CONSULTA_EMPLEADO",
                "VISTA_CITAS",
                "VISTA_CITAS_ADMIN",
                "VISTA_CITAS_EMPLEADO",
                "VISTA_HORARIOS_ADMIN",
                "VISTA_HORARIO_DETALLE_ADMIN",
                "VISTA_HORARIO_DETALLE",
                "VISTA_MI_HORARIO_EMPLEADO",
                "VISTA_EMPLEADO_DASHBOARD_OLD",
                "VISTA_PASSWORD_CHANGE",
                "VISTA_APODERADO",
                "VISTA_MIS_CITAS_APODERADO",
                "VISTA_MIS_MASCOTAS_APODERADO",
                "VISTA_APODERADO_MI_HISTORIAL",
                "VISTA_MIS_PAGOS_APODERADO",
                "VISTA_FACTURACION_ADMIN",
                "VISTA_FACTURACION",
                "VISTA_EMPRESA",
                "VISTA_PERFIL",
                "VISTA_LOGOUT",
                "VISTA_ROLES_LISTAR",
                "VISTA_ROLES_CREAR",
                "VISTA_ROLES_PERMISOS",
                "VISTA_ROLES_HISTORIAL",
                "VISTA_VISTAS_LISTAR",
                "VISTA_VISTAS_CREAR",
                "VISTA_VISTAS_CONFIGURAR",
                "VISTA_EMPLEADOS_LISTAR",
                "VISTA_EMPLEADOS_CREAR",
                "VISTA_EMPLEADOS_ROLES",
                "VISTA_EMPLEADOS_HORARIOS",
                "VISTA_MASCOTAS_LISTAR",
                "VISTA_MASCOTAS_CREAR",
                "VISTA_HISTORIAS_CLINICAS"
        );

        vistaRepository.findAll().stream()
                .filter(v -> codigosObsoletos.contains(v.getCodigo()))
                .forEach(v -> {
                    v.setActivo(false);
                    vistaRepository.save(v);
                });
    }

    private void deactivateObsoleteVentanas() {
        Set<String> codigosObsoletos = Set.of(
                "ADMIN",
                "ATENCION",
                "CLIENTES",
                "EMPLEADO",
                "PORTAL",
                "PERFIL",
                "CAMBIAR_CONTRASENA",
                "EMPRESA",
                "REPORTES",
                "HORARIOS",
                "COMPLEMENTARIO",
                "CITAS",
                "HISTORIAS",
                "RECETAS",
                "SERVICIOS",
                "APODERADOS",
                "FACTURACION",
                "INVENTARIO",
                "EMPLEADO_DASHBOARD",
                "MI_HORARIO",
                "APODERADO_DASHBOARD",
                "MIS_MASCOTAS",
                "MIS_CITAS",
                "MIS_PAGOS",
                "MIS_HISTORIAL",
                "DASHBOARD",
                "ROLES",
                "AUDITORIA",
                "ROLES_PERMISOS",
                "GESTION_VISTAS",
                "EMPLEADOS",
                "MASCOTAS"
        );

        ventanaRepository.findAll().stream()
                .filter(v -> codigosObsoletos.contains(v.getCodigo()))
                .forEach(v -> {
                    v.setActivo(false);
                    ventanaRepository.save(v);
                });
    }
}