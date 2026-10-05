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
    private final veterinaria.vargasvet.repository.RefreshTokenRepository refreshTokenRepository;

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Subscription> guardedSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByWebSocket = new ConcurrentHashMap<>();

    public void trackSession(WebSocketSession session) {
        sessionsById.put(session.getId(), session);
    }

    public void untrackSession(String sessionId) {
        sessionsById.remove(sessionId);
        guardedSubscriptions.remove(sessionId);
        sessionIdByWebSocket.remove(sessionId);
    }

    /** Asocia la conexión con la sesión de login que la autorizó (puede ser null en tickets antiguos). */
    public void bindSession(String webSocketSessionId, String loginSessionId) {
        if (webSocketSessionId != null && loginSessionId != null) {
            sessionIdByWebSocket.put(webSocketSessionId, loginSessionId);
        }
    }

    /** Cierra de inmediato las conexiones abiertas de una sesión que acaba de cerrarse. */
    public void closeConnectionsOfSession(String loginSessionId) {
        if (loginSessionId == null) return;
        sessionIdByWebSocket.forEach((webSocketId, sessionId) -> {
            if (loginSessionId.equals(sessionId)) {
                closeConnection(webSocketId, "Sesión cerrada");
            }
        });
    }

    private void closeConnection(String webSocketId, String reason) {
        WebSocketSession session = sessionsById.get(webSocketId);
        try {
            if (session != null && session.isOpen()) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
            }
        } catch (Exception e) {
            log.warn("No se pudo cerrar la sesión WebSocket {}: {}", webSocketId, reason);
        } finally {
            untrackSession(webSocketId);
        }
    }

    private void closeConnectionsOfRevokedSessions() {
        if (sessionIdByWebSocket.isEmpty()) return;
        java.util.Set<String> activeSessions = new java.util.HashSet<>(
                refreshTokenRepository.findActiveFamilyIds(new java.util.HashSet<>(sessionIdByWebSocket.values())));
        sessionIdByWebSocket.forEach((webSocketId, sessionId) -> {
            if (!activeSessions.contains(sessionId)) {
                closeConnection(webSocketId, "Sesión cerrada");
            }
        });
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
        closeConnectionsOfRevokedSessions();
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
