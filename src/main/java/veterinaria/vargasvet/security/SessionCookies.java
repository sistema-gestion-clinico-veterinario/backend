package veterinaria.vargasvet.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Cada clínica guarda su sesión en cookies propias (el nombre lleva su slug), de modo que un mismo navegador pueda
 * mantener sesiones de distintas clínicas a la vez sin que una reemplace a la otra. La cuenta de plataforma, que no
 * tiene clínica, sigue usando los nombres sin sufijo.
 */
public final class SessionCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    public static final String SLUG_HEADER = "X-Company-Slug";

    private static final String SEPARATOR = "__";
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,99}$");

    private SessionCookies() {
    }

    public static String accessName(String slug) {
        return slug == null ? ACCESS : ACCESS + SEPARATOR + slug;
    }

    public static String refreshName(String slug) {
        return slug == null ? REFRESH : REFRESH + SEPARATOR + slug;
    }

    /** El slug solo se acepta con la forma de un slug; cualquier otra cosa se ignora y no llega a un nombre de cookie. */
    public static String sanitize(String slug) {
        if (slug == null) {
            return null;
        }
        String limpio = slug.trim().toLowerCase(Locale.ROOT);
        return SLUG.matcher(limpio).matches() ? limpio : null;
    }

    public static String slugOf(HttpServletRequest request) {
        return sanitize(request.getHeader(SLUG_HEADER));
    }

    public static boolean isSessionCookie(String name) {
        return name != null && (name.equals(ACCESS) || name.equals(REFRESH)
                || name.startsWith(ACCESS + SEPARATOR) || name.startsWith(REFRESH + SEPARATOR));
    }

    public static Optional<String> readAccess(HttpServletRequest request) {
        return read(request, ACCESS);
    }

    public static Optional<String> readRefresh(HttpServletRequest request) {
        return read(request, REFRESH);
    }

    /**
     * Si la solicitud declara una clínica, solo cuenta la cookie de esa clínica. Si no declara una clínica, solo
     * cuenta la cookie sin sufijo de la plataforma. Nunca se infiere el contexto a partir de las cookies disponibles:
     * hacerlo permitiría que la pantalla de plataforma consumiera por accidente una sesión de clínica.
     */
    private static Optional<String> read(HttpServletRequest request, String base) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String declaredSlug = request.getHeader(SLUG_HEADER);
        if (declaredSlug != null) {
            String slug = sanitize(declaredSlug);
            if (slug == null) {
                return Optional.empty();
            }
            return valueOf(cookies, base + SEPARATOR + slug);
        }
        return valueOf(cookies, base);
    }

    private static Optional<String> valueOf(Cookie[] cookies, String name) {
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && hasValue(cookie)) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private static boolean hasValue(Cookie cookie) {
        return cookie.getValue() != null && !cookie.getValue().isBlank();
    }
}
