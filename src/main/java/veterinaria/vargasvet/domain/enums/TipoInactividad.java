package veterinaria.vargasvet.domain.enums;

/** Por qué una persona (empleado o cliente) no está activa en una empresa. */
public enum TipoInactividad {
    /** Bloqueo temporal: se espera que la persona vuelva; no se registra fecha de salida. */
    SUSPENSION,
    /** Fin de la relación con la empresa: se registra la fecha de salida del cliente. */
    BAJA
}
