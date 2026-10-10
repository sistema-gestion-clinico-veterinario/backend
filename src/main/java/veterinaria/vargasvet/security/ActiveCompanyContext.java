package veterinaria.vargasvet.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Contexto de empresa elegido por un administrador de plataforma para la
 * petición actual. No reemplaza la empresa incluida en una sesión de clínica:
 * únicamente completa el contexto de las sesiones globales.
 */
public final class ActiveCompanyContext {

    public static final String REQUEST_ATTRIBUTE = ActiveCompanyContext.class.getName() + ".companyId";

    private ActiveCompanyContext() {
    }

    public static void set(HttpServletRequest request, Integer companyId) {
        request.setAttribute(REQUEST_ATTRIBUTE, companyId);
    }

    public static Integer get() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            Object value = attributes.getRequest().getAttribute(REQUEST_ATTRIBUTE);
            return value instanceof Integer companyId ? companyId : null;
        }
        return null;
    }
}
