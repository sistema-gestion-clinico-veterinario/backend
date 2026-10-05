package veterinaria.vargasvet.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Optional;

/**
 * Identifica el equipo de un punto de cobro con un valor aleatorio guardado en una cookie HttpOnly. El navegador
 * nunca lo expone a scripts y el servidor solo conserva su hash; no se usan identificadores del hardware ni del
 * sistema que alguien pueda adivinar o copiar.
 *
 * <p>Cada sede tiene su propia cookie (el nombre lleva su slug): un mismo navegador puede ser el punto de cobro de
 * varias sedes sin que registrar una desvincule a la otra. Viaja a toda la API porque los cobros y las ventas no
 * están bajo la ruta de la caja.
 */
@Component
public class CajaDispositivoCookie {

    public static final String NAME = "caja_dispositivo";
    private static final String SEPARATOR = "__";
    private static final String PATH = "/api/v1";

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    public static String nombre(String slug) {
        String limpio = SessionCookies.sanitize(slug);
        return limpio == null ? NAME : NAME + SEPARATOR + limpio;
    }

    public Optional<String> leer(String slug) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        return leer(attributes.getRequest(), slug);
    }

    public Optional<String> leer(HttpServletRequest request, String slug) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String nombre = nombre(slug);
        for (Cookie cookie : cookies) {
            if (nombre.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public void fijar(HttpServletResponse response, String token, String slug) {
        escribir(response, nombre(slug), token, Duration.ofDays(365));
    }

    public void borrar(HttpServletResponse response, String slug) {
        escribir(response, nombre(slug), "", Duration.ZERO);
    }

    private void escribir(HttpServletResponse response, String nombre, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(nombre, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
