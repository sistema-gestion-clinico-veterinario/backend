package veterinaria.vargasvet.util;

import veterinaria.vargasvet.domain.entity.Usuario;

public final class CuentaPendiente {

    private CuentaPendiente() {
    }

    public static boolean es(Usuario usuario, boolean tieneContrasenaCreada) {
        return usuario != null && !usuario.isActivo() && !usuario.isEmailVerified() && !tieneContrasenaCreada;
    }
}
