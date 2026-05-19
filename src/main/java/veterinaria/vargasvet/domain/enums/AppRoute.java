package veterinaria.vargasvet.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppRoute {

    DASHBOARD("/dashboard", "Tablero Principal", "ADMIN"),
    ADMIN_COMPANY("/admin/company", "Gestión de Empresas (company)", "ADMIN"),
    ADMIN_EMPRESA("/admin/empresa", "Gestión de Empresas (empresa)", "ADMIN"),
    ADMIN_AUDITORIA("/admin/auditoria", "Auditorías del Sistema", "ADMIN"),
    ADMIN_ROLES("/admin/roles", "Roles y Permisos", "ADMIN"),
    ADMIN_MENUS("/admin/menus", "Gestión de Menús", "ADMIN"),
    ADMIN_COMPLEMENTARIO("/admin/complementario", "Datos Complementarios", "ADMIN"),
    ADMIN_EMPLEADOS("/admin/empleados", "Gestión de Empleados", "ADMIN"),
    ADMIN_CLIENTES("/admin/clientes", "Gestión de Clientes", "ADMIN"),
    ADMIN_PAGOS("/admin/pagos", "Historial de Pagos", "ADMIN"),
    ADMIN_HORARIOS("/admin/empleados/horarios", "Gestión de Horarios", "ADMIN"),
    ADMIN_REPORTS("/admin/reports", "Reportes", "ADMIN"),

    MASCOTAS("/mascotas", "Mascotas", "GENERAL"),
    RECETAS("/recetas", "Recetas", "GENERAL"),
    HISTORIAS_CLINICAS("/historias-clinicas", "Historias Clínicas", "GENERAL"),
    CITAS_AGENDA("/citas/agenda", "Agenda de Citas", "GENERAL"),
    MI_HORARIO("/mi-horario", "Mi Horario", "EMPLEADO"),
    EMPLEADO_DASHBOARD("/empleado/dashboard", "Tablero Empleado", "EMPLEADO"),
    PROFILE("/profile", "Mi Perfil", "GENERAL"),

    APODERADO_DASHBOARD("/apoderado/dashboard", "Tablero Apoderado", "APODERADO"),
    APODERADO_PORTAL("/apoderado", "Portal Apoderado", "APODERADO"),
    MIS_CITAS("/mis-citas", "Mis Citas", "APODERADO"),
    MIS_MASCOTAS("/mis-mascotas", "Mis Mascotas", "APODERADO"),
    MI_HISTORIAL("/mi-historial", "Mi Historial Clínico", "APODERADO"),
    MIS_PAGOS("/mis-pagos", "Mis Pagos", "APODERADO");

    private final String path;
    private final String label;
    private final String group;
}
