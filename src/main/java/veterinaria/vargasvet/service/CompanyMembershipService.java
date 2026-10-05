package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.entity.Usuario;

import java.util.Set;

/**
 * Unico lugar donde vive la logica de "en que empresas esta activo un usuario".
 * Fuente de verdad: Empleado.company/apoderado.company/UsuarioMembresia.company,
 * cada una con su propio estado - nunca Usuario.company (que a partir de esta
 * migracion es solo una cache de compatibilidad, ver syncLegacyCompanyField).
 */
public interface CompanyMembershipService {

    /**
     * Empresas donde el usuario tiene una relacion ACTIVA ahora mismo, sea como
     * Empleado, Apoderado o UsuarioMembresia. Usar en login, seleccion de empresa,
     * refresh y switch-company - no en el camino por-request (ver hasActiveMembership).
     */
    Set<Integer> getActiveCompanyIds(Usuario usuario);

    /**
     * Comprobacion de existencia puntual y barata: ¿el usuario tiene una relacion
     * activa con ESTA empresa en particular? Pensada para JWTFilter y cualquier otro
     * chequeo por-request - nunca carga ni enumera el resto de membresias del usuario.
     */
    boolean hasActiveMembership(Integer usuarioId, Integer companyId);

    /** ¿El usuario es empleado ACTIVO de esta empresa? Un cliente activo no cuenta: sirve para decidir quién puede
     * ejercer un rol de personal, como el de administrador. */
    boolean hasActiveStaffMembership(Integer usuarioId, Integer companyId);

    /** ¿El usuario tiene alguna relación con ESTA empresa, sin importar si está activa, suspendida o dada de baja? */
    boolean hasAnyMembership(Integer usuarioId, Integer companyId);

    /** ¿La relación del usuario con ESTA empresa está suspendida (no dada de baja)? */
    boolean isSuspendedIn(Integer usuarioId, Integer companyId);

    /** ¿La relación del usuario con ESTA empresa fue dada de baja? */
    boolean isDeactivatedIn(Integer usuarioId, Integer companyId);

    /**
     * true si el usuario tiene al menos una relacion (Empleado, Apoderado o
     * UsuarioMembresia) y TODAS estan desactivadas. Una cuenta sin ninguna relacion
     * (p. ej. administrador de plataforma) devuelve false: no fue desactivada por una
     * empresa. Sirve para impedir que quien fue dado de baja antes de activar su cuenta
     * la active por su cuenta con un enlace pendiente o reenviado.
     */
    boolean hasOnlyInactiveMemberships(Integer usuarioId);

    /**
     * ¿Otro usuario ya usa este correo en alguna de las empresas donde el usuario tiene una
     * relación activa? Se resuelve por membresías (como el login) y no por el campo
     * Usuario.company, que queda vacío cuando la persona pertenece a más de una empresa.
     */
    boolean isEmailTakenInUserCompanies(Usuario usuario, String email);

    /** Igual que {@link #isEmailTakenInUserCompanies} pero para el nombre de usuario. */
    boolean isUsernameTakenInUserCompanies(Usuario usuario, String username);

    /**
     * Unico metodo autorizado para escribir Usuario.company (caché de compatibilidad
     * legacy). Debe llamarse dentro de la MISMA transaccion que crea/modifica una
     * membresia (Empleado/Apoderado/UsuarioMembresia), nunca despues del commit.
     * Deja Usuario.company en la unica empresa activa si hay exactamente una, o en
     * null si hay cero o varias - nunca elige una arbitrariamente entre varias.
     */
    void syncLegacyCompanyField(Usuario usuario);
}
