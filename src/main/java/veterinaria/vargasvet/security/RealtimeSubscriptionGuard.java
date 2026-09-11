package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Revalida periódicamente los permisos de las suscripciones WebSocket sensibles (auditoría) y
 * cierra la sesión si el permiso fue revocado mientras la conexión seguía abierta. La validación
 * en {@code SUBSCRIBE} (ver {@code WebSocketConfig}) solo se ejecuta una vez, al momento de
 * suscribirse; sin este guard, una conexión ya autorizada seguiría recibiendo eventos aunque al
 * usuario le quiten el permiso después.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeSubscriptionGuard {

    private final RolePermissionEvaluator rolePermissionEvaluator;

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Subscription> guardedSubscriptions = new ConcurrentHashMap<>();

    public void trackSession(WebSocketSession session) {
        sessionsById.put(session.getId(), session);
    }

    public void untrackSession(String sessionId) {
        sessionsById.remove(sessionId);
        guardedSubscriptions.remove(sessionId);
    }

    /**
     * Registra una suscripción para revalidación continua. Solo debe usarse para canales donde
     * perder el permiso en caliente sea sensible (p. ej. auditoría), no para todos los topics.
     */
    public void guard(String sessionId, Integer userId, Integer roleId, String viewCode, String action) {
        guardedSubscriptions.put(sessionId, new Subscription(userId, roleId, viewCode, action));
    }

    @Scheduled(fixedDelay = 30_000)
    public void revalidate() {
        guardedSubscriptions.forEach((sessionId, subscription) -> {
            boolean stillAllowed = rolePermissionEvaluator.can(
                    subscription.userId(), subscription.roleId(), subscription.viewCode(), subscription.action());
            if (stillAllowed) return;

            WebSocketSession session = sessionsById.get(sessionId);
            if (session == null) {
                guardedSubscriptions.remove(sessionId);
                return;
            }
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Permiso revocado"));
            } catch (Exception e) {
                log.warn("No se pudo cerrar la sesión WebSocket {} tras revocar el permiso", sessionId);
            } finally {
                untrackSession(sessionId);
            }
        });
    }

    private record Subscription(Integer userId, Integer roleId, String viewCode, String action) {
    }
}
