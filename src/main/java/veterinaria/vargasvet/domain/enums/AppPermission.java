package veterinaria.vargasvet.domain.enums;

import lombok.Getter;

@Getter
public enum AppPermission {
    // Módulo de Citas
    CITA_READ("CITAS", "Ver Agenda", "Permite visualizar el calendario de citas"),
    CITA_CREATE("CITAS", "Agendar Cita", "Permite registrar nuevas citas médicas"),
    CITA_UPDATE("CITAS", "Editar Cita", "Permite modificar datos de una cita existente"),
    CITA_CANCEL("CITAS", "Cancelar Cita", "Permite anular citas programadas"),

    // Módulo de Pacientes (Mascotas)
    PET_READ("MASCOTAS", "Ver Pacientes", "Acceso a la lista y perfiles de mascotas"),
    PET_WRITE("MASCOTAS", "Gestionar Pacientes", "Permite crear y editar perfiles de mascotas"),
    PET_HISTORY_READ("MASCOTAS", "Ver Historias Clínicas", "Permite visualizar el historial médico completo"),
    PET_HISTORY_WRITE("MASCOTAS", "Registrar Consulta", "Permite añadir nuevas consultas a la historia clínica"),

    // Módulo de Clientes (Apoderados)
    CLIENT_READ("CLIENTES", "Ver Clientes", "Acceso al directorio de dueños de mascotas"),
    CLIENT_WRITE("CLIENTES", "Gestionar Clientes", "Permite crear y editar información de contacto de clientes"),

    // Módulo de Inventario
    INV_READ("INVENTARIO", "Ver Inventario", "Visualización de productos y stock"),
    INV_WRITE("INVENTARIO", "Gestionar Stock", "Permite realizar entradas, salidas y ajustes de inventario"),

    // Módulo Administrativo y Configuración
    ADMIN_DASHBOARD("ADMIN", "Ver Dashboard", "Acceso a estadísticas y métricas generales"),
    USER_MANAGE("ADMIN", "Gestionar Usuarios", "Administración total de cuentas, empleados y roles"),
    COMPANY_MANAGE("ADMIN", "Configurar Empresa", "Ajustes de la clínica (horarios, sedes, etc)");

    private final String module;
    private final String label;
    private final String description;

    AppPermission(String module, String label, String description) {
        this.module = module;
        this.label = label;
        this.description = description;
    }
}
