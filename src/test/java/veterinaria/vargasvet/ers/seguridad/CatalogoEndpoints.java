package veterinaria.vargasvet.ers.seguridad;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Todos los endpoints de la API, leídos de los controladores, con la regla de autorización que declaran. */
final class CatalogoEndpoints {

    /** Rutas abiertas a propósito en WebSecurityConfig: se usan antes de iniciar sesión o son información pública. */
    static final Set<String> PUBLICOS = Set.of(
            "GET /auth/google/callback", "GET /auth/google/start", "GET /auth/validate-reset-token",
            "GET /company/branding/{slug}", "GET /company/search", "GET /health", "GET /legal/current",
            "GET /media/{filename:.+}", "GET /public/legal/{slug}", "GET /public/privacidad/{slug}", "GET /public/privacidad/{slug}/version/{version}",
            "POST /auth/account/reactivate", "POST /auth/admin-login", "POST /auth/email-change/cancel",
            "POST /auth/email-change/confirm-current", "POST /auth/email-change/confirm-new", "POST /auth/forgot-password",
            "POST /auth/google/exchange", "POST /auth/google/intent", "POST /auth/google/reactivate", "POST /auth/login",
            "POST /auth/logout", "POST /auth/refresh", "POST /auth/resend-verification",
            "POST /auth/resend-verification-by-token", "POST /auth/reset-password", "POST /auth/setup-account",
            "POST /auth/validate-reset-token");

    /** Exigen sesión válida y actúan solo sobre la cuenta de quien llama: no reciben el identificador de otra persona. */
    static final Set<String> AUTOSERVICIO = Set.of(
            "GET /auth/account/closure", "GET /auth/session", "GET /legal/my-acceptances", "GET /legal/status",
            "GET /privacidad/aviso-vigente", "GET /privacidad/mi-estado", "GET /profile", "GET /profile/schedule",
            "POST /auth/account/closure/confirm", "POST /auth/account/closure/request", "POST /auth/change-password",
            "POST /auth/email-change/request", "POST /auth/switch-role", "POST /legal/accept", "POST /privacidad/enterado",
            "POST /realtime/ticket", "PUT /privacidad/finalidades/{finalidad}", "PUT /profile");

    /** Canales máquina-a-máquina autenticados por un filtro antes de resolver el controlador. */
    static final Set<String> INTERNOS_CON_TOKEN = Set.of("POST /internal/backups/events");

    private static final Pattern VARIABLE = Pattern.compile("\\{([^}:]+)(?::[^}]*)?}");

    record Endpoint(String verbo, String ruta, Class<?> controlador, Method metodo, String regla) {
        String clave() { return verbo + " " + ruta; }
        boolean esPublico() { return PUBLICOS.contains(clave()); }
        boolean esAutoservicio() { return AUTOSERVICIO.contains(clave()); }
        boolean esInternoConToken() { return INTERNOS_CON_TOKEN.contains(clave()); }
        boolean tieneVariables() { return ruta.contains("{"); }
        String descripcion() { return clave() + "  [" + controlador.getSimpleName() + "." + metodo.getName() + "]"; }

        List<String> variables() {
            List<String> nombres = new ArrayList<>();
            Matcher m = VARIABLE.matcher(ruta);
            while (m.find()) nombres.add(m.group(1));
            return nombres;
        }
    }

    private CatalogoEndpoints() {}

    static List<Endpoint> todos() {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Endpoint> endpoints = new ArrayList<>();
        try {
            for (var definicion : escaner.findCandidateComponents("veterinaria.vargasvet")) {
                Class<?> controlador = Class.forName(definicion.getBeanClassName());
                PreAuthorize deClase = controlador.getAnnotation(PreAuthorize.class);
                String prefijo = prefijo(controlador);
                for (Method metodo : controlador.getDeclaredMethods()) {
                    String verbo = verbo(metodo);
                    if (verbo == null) continue;
                    PreAuthorize regla = metodo.getAnnotation(PreAuthorize.class) != null
                            ? metodo.getAnnotation(PreAuthorize.class) : deClase;
                    endpoints.add(new Endpoint(verbo, prefijo + ruta(metodo), controlador, metodo,
                            regla == null ? null : regla.value().trim()));
                }
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
        endpoints.sort(Comparator.comparing(Endpoint::clave));
        return endpoints;
    }

    static String sustituir(String ruta, java.util.function.Function<String, String> valorDe) {
        Matcher m = VARIABLE.matcher(ruta);
        StringBuilder resultado = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(resultado, Matcher.quoteReplacement(valorDe.apply(m.group(1))));
        }
        m.appendTail(resultado);
        return resultado.toString();
    }

    private static String prefijo(Class<?> controlador) {
        RequestMapping base = controlador.getAnnotation(RequestMapping.class);
        return base != null && base.value().length > 0 ? base.value()[0] : "";
    }

    static String verbo(Method m) {
        if (m.isAnnotationPresent(GetMapping.class)) return "GET";
        if (m.isAnnotationPresent(PostMapping.class)) return "POST";
        if (m.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (m.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (m.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        if (m.isAnnotationPresent(RequestMapping.class)) return "GET";
        return null;
    }

    private static String ruta(Method m) {
        String[] v = new String[0];
        if (m.isAnnotationPresent(GetMapping.class)) v = m.getAnnotation(GetMapping.class).value();
        else if (m.isAnnotationPresent(PostMapping.class)) v = m.getAnnotation(PostMapping.class).value();
        else if (m.isAnnotationPresent(PutMapping.class)) v = m.getAnnotation(PutMapping.class).value();
        else if (m.isAnnotationPresent(DeleteMapping.class)) v = m.getAnnotation(DeleteMapping.class).value();
        else if (m.isAnnotationPresent(PatchMapping.class)) v = m.getAnnotation(PatchMapping.class).value();
        else if (m.isAnnotationPresent(RequestMapping.class)) v = m.getAnnotation(RequestMapping.class).value();
        return v.length > 0 ? v[0] : "";
    }
}
