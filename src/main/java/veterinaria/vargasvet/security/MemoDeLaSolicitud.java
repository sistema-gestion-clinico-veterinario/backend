package veterinaria.vargasvet.security;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.function.Supplier;

public final class MemoDeLaSolicitud {

    private MemoDeLaSolicitud() {
    }

    public static boolean hayUnaSolicitudActiva() {
        return solicitudActiva() != null;
    }

    public static <T> T obtener(String clave, Class<T> tipo, Supplier<T> calculo) {
        ServletRequestAttributes solicitud = solicitudActiva();
        if (solicitud == null) {
            return calculo.get();
        }
        Object guardado = solicitud.getAttribute(clave, RequestAttributes.SCOPE_REQUEST);
        if (tipo.isInstance(guardado)) {
            return tipo.cast(guardado);
        }
        T valor = calculo.get();
        solicitud.setAttribute(clave, valor, RequestAttributes.SCOPE_REQUEST);
        return valor;
    }

    private static ServletRequestAttributes solicitudActiva() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos ? atributos : null;
    }
}
