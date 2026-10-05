package veterinaria.vargasvet.util;

public final class DispositivoInfo {

    private DispositivoInfo() {
    }

    /** Descripción corta y legible del equipo ("Chrome en Windows"). No identifica a nadie ni sirve como credencial. */
    public static String describir(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Equipo sin identificar";
        }
        String ua = userAgent.toLowerCase(java.util.Locale.ROOT);
        String navegador = ua.contains("edg/") ? "Edge"
                : ua.contains("opr/") || ua.contains("opera") ? "Opera"
                : ua.contains("firefox") ? "Firefox"
                : ua.contains("chrome") || ua.contains("crios") ? "Chrome"
                : ua.contains("safari") ? "Safari"
                : "Navegador";
        String sistema = ua.contains("android") ? "Android"
                : ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios") ? "iOS"
                : ua.contains("windows") ? "Windows"
                : ua.contains("mac os") || ua.contains("macintosh") ? "macOS"
                : ua.contains("linux") ? "Linux"
                : "sistema desconocido";
        return navegador + " en " + sistema;
    }
}
