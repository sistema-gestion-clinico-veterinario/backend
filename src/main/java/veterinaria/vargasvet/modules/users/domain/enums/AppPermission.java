package veterinaria.vargasvet.modules.users.domain.enums;

import lombok.Getter;

@Getter
public enum AppPermission {
    // --- NUEVOS PERMISOS (Sincronizados con tu DB) ---
    USER_READ("SISTEMA", "Leer Usuarios", "Permite ver la lista de usuarios"),
    USER_CREATE("SISTEMA", "Crear Usuario", "Permite registrar nuevos usuarios"),
    USER_UPDATE("SISTEMA", "Editar Usuario", "Permite modificar usuarios existentes"),
    USER_DELETE("SISTEMA", "Eliminar Usuario", "Permite dar de baja usuarios"),
    ROLE_MANAGE("SISTEMA", "Gestionar Roles", "Permite administrar roles y sus permisos"),

    PET_CREATE("MASCOTAS", "Crear Mascota", "Permite registrar nuevas mascotas"),
    PET_UPDATE("MASCOTAS", "Editar Mascota", "Permite modificar datos de mascotas"),
    PET_DELETE("MASCOTAS", "Eliminar Mascota", "Permite eliminar registros de mascotas"),

    CLINICAL_RECORD_READ("HISTORIAL", "Ver Historias", "Acceso a consulta de historias clínicas"),
    CLINICAL_RECORD_MANAGE("HISTORIAL", "Gestionar Historias", "Administración total de registros clínicos"),

    VETERINARY_PRACTICE("VET", "Práctica Veterinaria", "Acceso a funciones específicas de veterinarios"),
    SYSTEM_CONFIG("SISTEMA", "Configuración", "Acceso a ajustes técnicos del sistema"),

    INVENTORY_READ("INVENTARIO", "Ver Inventario", "Consulta de stock y productos"),
    INVENTORY_MANAGE("INVENTARIO", "Gestionar Inventario", "Control de entradas y salidas de stock"),

    SALE_READ("VENTAS", "Ver Ventas", "Consulta de registros de ventas"),
    SALE_MANAGE("VENTAS", "Gestionar Ventas", "Permite realizar y anular ventas"),
    PURCHASE_MANAGE("COMPRAS", "Gestionar Compras", "Control de compras a proveedores"),

    // --- PERMISOS EXISTENTES ---
    CITA_READ("CITAS", "Ver Agenda", "Permite visualizar el calendario de citas"),
    CITA_CREATE("CITAS", "Agendar Cita", "Permite registrar nuevas citas médicas"),
    CITA_UPDATE("CITAS", "Editar Cita", "Permite modificar datos de una cita existente"),
    CITA_CANCEL("CITAS", "Cancelar Cita", "Permite anular citas programadas"),

    PET_READ("MASCOTAS", "Ver Pacientes", "Acceso a la lista y perfiles de mascotas"),
    PET_WRITE("MASCOTAS", "Gestionar Pacientes", "Permite crear y editar perfiles de mascotas"),
    PET_HISTORY_READ("MASCOTAS", "Ver Historias Clínicas", "Permite visualizar el historial médico completo"),
    PET_HISTORY_WRITE("MASCOTAS", "Registrar Consulta", "Permite añadir nuevas consultas a la historia clínica"),

    CLIENT_READ("CLIENTES", "Ver Clientes", "Acceso al directorio de dueños de mascotas"),
    CLIENT_WRITE("CLIENTES", "Gestionar Clientes", "Permite crear y editar información de contacto de clientes"),

    INV_READ("INVENTARIO", "Ver Inventario", "Visualización de productos y stock"),
    INV_WRITE("INVENTARIO", "Gestionar Stock", "Permite realizar entradas, salidas y ajustes de inventario"),

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
