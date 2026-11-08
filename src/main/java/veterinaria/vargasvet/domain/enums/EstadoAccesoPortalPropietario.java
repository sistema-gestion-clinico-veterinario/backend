package veterinaria.vargasvet.domain.enums;

/** Estado del acceso digital del propietario. Es independiente de su estado
 * como cliente y de la conservación de sus datos clínicos y administrativos. */
public enum EstadoAccesoPortalPropietario {
    SIN_ACCESO,
    INVITACION_PENDIENTE,
    ACTIVO,
    SUSPENDIDO,
    REVOCADO
}
