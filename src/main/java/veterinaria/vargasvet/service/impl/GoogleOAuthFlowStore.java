package veterinaria.vargasvet.service.impl;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.exception.RateLimitExceededException;
import veterinaria.vargasvet.security.SecurityTokenUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Estado de un inicio de sesión (o activación) con Google, guardado del lado del servidor para que
 * ni el slug ni el token de invitación viajen por la URL que pasa por Google. En memoria: alcanza
 * porque los códigos viven minutos y el backend corre en una sola instancia. */
@Component
public class GoogleOAuthFlowStore {

    private static final Duration INTENT_TTL = Duration.ofMinutes(5);
    private static final Duration AUTHORIZATION_TTL = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 10_000;

    public record Context(String slug, String activationToken) {
        public boolean isActivation() {
            return activationToken != null;
        }
    }

    public record Authorization(Context context, String codeVerifier) {}

    private record Entry<T>(T value, Instant expiresAt) {}

    private Clock clock = Clock.systemUTC();
    private final Map<String, Entry<Context>> intents = new ConcurrentHashMap<>();
    private final Map<String, Entry<Authorization>> authorizations = new ConcurrentHashMap<>();

    public String createIntent(Context context) {
        return put(intents, context, INTENT_TTL);
    }

    public Context consumeIntent(String intent) {
        return take(intents, intent);
    }

    public String createAuthorization(Authorization authorization) {
        return put(authorizations, authorization, AUTHORIZATION_TTL);
    }

    public Authorization consumeAuthorization(String state) {
        return take(authorizations, state);
    }

    private <T> String put(Map<String, Entry<T>> map, T value, Duration ttl) {
        Instant now = Instant.now(clock);
        map.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (map.size() >= MAX_ENTRIES) {
            throw new RateLimitExceededException();
        }
        String key = SecurityTokenUtils.generate();
        map.put(key, new Entry<>(value, now.plus(ttl)));
        return key;
    }

    private <T> T take(Map<String, Entry<T>> map, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Entry<T> entry = map.remove(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now(clock))) {
            return null;
        }
        return entry.value();
    }
}
