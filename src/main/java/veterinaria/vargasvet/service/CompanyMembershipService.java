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

    /**
     * Unico metodo autorizado para escribir Usuario.company (caché de compatibilidad
     * legacy). Debe llamarse dentro de la MISMA transaccion que crea/modifica una
     * membresia (Empleado/Apoderado/UsuarioMembresia), nunca despues del commit.
     * Deja Usuario.company en la unica empresa activa si hay exactamente una, o en
     * null si hay cero o varias - nunca elige una arbitrariamente entre varias.
     */
    void syncLegacyCompanyField(Usuario usuario);

    /**
     * Lanza IllegalArgumentException si el usuario ya tiene una relacion laboral
     * (Empleado) activa en CUALQUIER empresa. Un empleado no puede tener dos
     * relaciones laborales activas simultaneas en el sistema - si la empresa
     * anterior no lo dio de baja, este metodo bloquea con un mensaje claro; no
     * hace ninguna accion automatica (ni Super Admin ni autoconfirmacion). Usar
     * antes de registrar un Empleado/Veterinario para un usuario con email ya
     * existente. No aplica a Apoderado (si puede estar activo en varias empresas).
     */
    void assertNoActiveEmploymentElsewhere(Usuario usuario);
}
