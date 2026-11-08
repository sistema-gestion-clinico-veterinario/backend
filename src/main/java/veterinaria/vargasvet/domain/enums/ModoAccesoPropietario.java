package veterinaria.vargasvet.domain.enums;

/**
 * Define si los propietarios son solo registros operativos o también usuarios
 * del portal de una clínica.
 */
public enum ModoAccesoPropietario {
    SOLO_GESTION_INTERNA,
    PORTAL_OPCIONAL,
    PORTAL_POR_DEFECTO
}
