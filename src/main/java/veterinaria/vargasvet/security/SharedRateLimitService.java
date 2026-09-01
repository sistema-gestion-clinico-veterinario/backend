package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.exception.RateLimitExceededException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Contador atomico compartido para operaciones de autenticacion. Se apoya en
 * PostgreSQL porque es infraestructura ya obligatoria para la aplicacion; el
 * trafico general permanece limitado en memoria para no convertir cada request
 * en una escritura de base de datos.
 */
@Service
@RequiredArgsConstructor
public class SharedRateLimitService {

    private static final String CONSUME_SQL = """
            INSERT INTO security_rate_limits (rate_key, window_started_at, attempts, expires_at)
            VALUES (?, ?, 1, ?)
            ON CONFLICT (rate_key) DO UPDATE SET
                attempts = CASE
                    WHEN security_rate_limits.window_started_at < EXCLUDED.window_started_at THEN 1
                    ELSE security_rate_limits.attempts + 1
                END,
                window_started_at = CASE
                    WHEN security_rate_limits.window_started_at < EXCLUDED.window_started_at
                        THEN EXCLUDED.window_started_at
                    ELSE security_rate_limits.window_started_at
                END,
                expires_at = EXCLUDED.expires_at
            RETURNING attempts
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryConsume(String namespace, String identifier, int capacity, Duration period) {
        if (capacity < 1 || period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("La configuracion del rate limit no es valida");
        }

        String safeIdentifier = identifier == null || identifier.isBlank() ? "unknown" : identifier.trim();
        String key = SecurityTokenUtils.hash(namespace + ':' + safeIdentifier);
        Instant now = Instant.now();
        long periodSeconds = period.toSeconds();
        Instant windowStart = Instant.ofEpochSecond((now.getEpochSecond() / periodSeconds) * periodSeconds);
        Instant expiresAt = windowStart.plus(period).plus(period);

        Integer attempts = jdbcTemplate.queryForObject(
                CONSUME_SQL,
                Integer.class,
                key,
                Timestamp.from(windowStart),
                Timestamp.from(expiresAt)
        );
        return attempts != null && attempts <= capacity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enforce(String namespace, String identifier, int capacity, Duration period) {
        if (!tryConsume(namespace, identifier, capacity, period)) {
            throw new RateLimitExceededException();
        }
    }

    @Scheduled(cron = "${app.rate-limit.cleanup-cron:0 17 * * * *}")
    public void deleteExpiredCounters() {
        jdbcTemplate.update("DELETE FROM security_rate_limits WHERE expires_at < CURRENT_TIMESTAMP");
    }
}
