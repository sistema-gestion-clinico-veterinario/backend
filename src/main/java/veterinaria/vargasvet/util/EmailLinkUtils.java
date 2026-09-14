package veterinaria.vargasvet.util;

/**
 * Construye rutas de enlaces de correo (activacion, reset de contrasena, cambio
 * de correo) incluyendo el slug de la empresa, para que el enlace aterrice en
 * la pantalla de login/branding correcta. Ver plan de login por slug de empresa.
 */
public final class EmailLinkUtils {

    private EmailLinkUtils() {
    }

    /**
     * Antepone el slug de empresa (si existe) al path dado.
     * Ej: withSlug("/auth/verify#token=abc", "michi") -> "/michi/auth/verify#token=abc"
     */
    public static String withSlug(String path, String slug) {
        String prefix = (slug == null || slug.isBlank()) ? "" : "/" + slug;
        return prefix + path;
    }
}
