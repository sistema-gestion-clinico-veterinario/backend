package veterinaria.vargasvet.domain.enums;

import lombok.Getter;

@Getter
public enum AppPermission {

    // SISTEMA
    USER_READ("SISTEMA", "Ver Usuarios", "Permite ver la lista de usuarios"),
    USER_CREATE("SISTEMA", "Crear Usuario", "Permite registrar nuevos usuarios"),
    USER_UPDATE("SISTEMA", "Editar Usuario", "Permite modificar usuarios existentes"),
    USER_DELETE("SISTEMA", "Eliminar Usuario", "Permite dar de baja usuarios"),
    ROLE_MANAGE("SISTEMA", "Gestionar Roles", "Permite administrar roles y sus permisos"),

    // EMPRESA
    COMPANY_READ("EMPRESA", "Ver Empresa", "Permite ver datos de la empresa"),
    COMPANY_CREATE("EMPRESA", "Crear Empresa", "Permite registrar nuevas empresas"),
    COMPANY_UPDATE("EMPRESA", "Editar Empresa", "Permite modificar datos de la empresa"),
    COMPANY_MANAGE("EMPRESA", "Configurar Empresa", "Administración total de la empresa"),

    // EMPLEADOS
    EMPLEADO_READ("EMPLEADOS", "Ver Empleados", "Permite ver la lista de empleados"),
    EMPLEADO_CREATE("EMPLEADOS", "Crear Empleado", "Permite registrar nuevos empleados"),
    EMPLEADO_UPDATE("EMPLEADOS", "Editar Empleado", "Permite modificar datos de empleados"),
    EMPLEADO_STATUS("EMPLEADOS", "Cambiar Estado Empleado", "Permite activar o desactivar empleados"),
    EMPLEADO_DELETE("EMPLEADOS", "Eliminar Empleado", "Permite eliminar empleados del sistema"),

    // TIPOS DE EMPLEADO
    TIPO_EMPLEADO_READ("EMPLEADOS", "Ver Tipos de Empleado", "Permite ver tipos de empleado"),
    TIPO_EMPLEADO_CREATE("EMPLEADOS", "Crear Tipo de Empleado", "Permite crear tipos de empleado"),
    TIPO_EMPLEADO_UPDATE("EMPLEADOS", "Editar Tipo de Empleado", "Permite editar tipos de empleado"),
    TIPO_EMPLEADO_STATUS("EMPLEADOS", "Cambiar Estado Tipo Empleado", "Permite activar o desactivar tipos de empleado"),
    TIPO_EMPLEADO_DELETE("EMPLEADOS", "Eliminar Tipo de Empleado", "Permite eliminar tipos de empleado"),

    // ESPECIALIDADES
    ESPECIALIDAD_READ("EMPLEADOS", "Ver Especialidades", "Permite ver especialidades"),
    ESPECIALIDAD_CREATE("EMPLEADOS", "Crear Especialidad", "Permite crear especialidades"),
    ESPECIALIDAD_UPDATE("EMPLEADOS", "Editar Especialidad", "Permite editar especialidades"),
    ESPECIALIDAD_DELETE("EMPLEADOS", "Eliminar Especialidad", "Permite eliminar especialidades"),

    // CLIENTES / APODERADOS
    APODERADO_READ("CLIENTES", "Ver Clientes", "Permite ver la lista de clientes/apoderados"),
    APODERADO_CREATE("CLIENTES", "Crear Cliente", "Permite registrar nuevos clientes"),
    APODERADO_UPDATE("CLIENTES", "Editar Cliente", "Permite modificar datos de clientes"),
    APODERADO_STATUS("CLIENTES", "Cambiar Estado Cliente", "Permite activar o desactivar clientes"),
    APODERADO_DELETE("CLIENTES", "Eliminar Cliente", "Permite eliminar clientes del sistema"),

    // MASCOTAS
    PET_READ("MASCOTAS", "Ver Mascotas", "Acceso a la lista y perfiles de mascotas"),
    PET_CREATE("MASCOTAS", "Crear Mascota", "Permite registrar nuevas mascotas"),
    PET_UPDATE("MASCOTAS", "Editar Mascota", "Permite modificar datos de mascotas"),
    PET_STATUS("MASCOTAS", "Cambiar Estado Mascota", "Permite activar o dar de baja mascotas"),
    PET_DELETE("MASCOTAS", "Eliminar Mascota", "Permite eliminar registros de mascotas"),
    PET_WRITE("MASCOTAS", "Gestionar Mascotas", "Permite crear y editar perfiles de mascotas"),
    PET_HISTORY_READ("MASCOTAS", "Ver Historias Clínicas", "Permite visualizar el historial médico completo"),
    PET_HISTORY_WRITE("MASCOTAS", "Registrar Consulta", "Permite añadir nuevas consultas a la historia clínica"),

    // HISTORIAS CLÍNICAS
    CLINICAL_RECORD_READ("HISTORIAL", "Ver Historias", "Acceso a consulta de historias clínicas"),
    CLINICAL_RECORD_MANAGE("HISTORIAL", "Gestionar Historias", "Administración total de registros clínicos"),

    // CITAS
    CITA_READ("CITAS", "Ver Agenda", "Permite visualizar el calendario de citas"),
    CITA_CREATE("CITAS", "Agendar Cita", "Permite registrar nuevas citas médicas"),
    CITA_UPDATE("CITAS", "Editar Cita", "Permite modificar datos de una cita existente"),
    CITA_CANCEL("CITAS", "Cancelar Cita", "Permite anular citas programadas"),
    CITA_INICIAR("CITAS", "Iniciar Atención", "Permite iniciar la atención de una cita"),
    CITA_DELETE("CITAS", "Eliminar Cita", "Permite eliminar citas del sistema"),

    // SERVICIOS
    SERVICIO_READ("SERVICIOS", "Ver Servicios", "Permite ver el catálogo de servicios"),
    SERVICIO_CREATE("SERVICIOS", "Crear Servicio", "Permite registrar nuevos servicios"),
    SERVICIO_UPDATE("SERVICIOS", "Editar Servicio", "Permite modificar servicios existentes"),
    SERVICIO_DELETE("SERVICIOS", "Eliminar Servicio", "Permite desactivar servicios"),
    SERVICIO_TOGGLE("SERVICIOS", "Activar/Desactivar Servicio", "Permite cambiar disponibilidad de servicios"),

    // INVENTARIO
    INV_READ("INVENTARIO", "Ver Inventario", "Visualización de productos y stock"),
    INV_WRITE("INVENTARIO", "Gestionar Stock", "Permite realizar entradas, salidas y ajustes de inventario"),
    INVENTORY_READ("INVENTARIO", "Ver Inventario", "Consulta de stock y productos"),
    INVENTORY_MANAGE("INVENTARIO", "Gestionar Inventario", "Control de entradas y salidas de stock"),

    // VENTAS / COMPRAS
    SALE_READ("VENTAS", "Ver Ventas", "Consulta de registros de ventas"),
    SALE_MANAGE("VENTAS", "Gestionar Ventas", "Permite realizar y anular ventas"),
    PURCHASE_MANAGE("COMPRAS", "Gestionar Compras", "Control de compras a proveedores"),

    // OTROS
    CLIENT_READ("CLIENTES", "Ver Clientes (legacy)", "Acceso al directorio de dueños de mascotas"),
    CLIENT_WRITE("CLIENTES", "Gestionar Clientes (legacy)", "Permite crear y editar información de clientes"),
    VETERINARY_PRACTICE("VET", "Práctica Veterinaria", "Acceso a funciones específicas de veterinarios"),
    SYSTEM_CONFIG("SISTEMA", "Configuración", "Acceso a ajustes técnicos del sistema"),
    ADMIN_DASHBOARD("ADMIN", "Ver Dashboard", "Acceso a estadísticas y métricas generales"),
    USER_MANAGE("ADMIN", "Gestionar Usuarios", "Administración total de cuentas, empleados y roles");

    private final String module;
    private final String label;
    private final String description;

    AppPermission(String module, String label, String description) {
        this.module = module;
        this.label = label;
        this.description = description;
    }
}
