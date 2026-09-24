package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.account-lockout.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.account-lockout.lockout-minutes:30}")
    private int lockoutMinutes;

    public void assertNotLocked(String identifier) {
        String key = accountKey(identifier);
        Timestamp lockedUntil = jdbcTemplate.query(
                "SELECT locked_until FROM account_lockouts WHERE account_key = ?",
                rs -> rs.next() ? rs.getTimestamp("locked_until") : null,
                key);
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(Instant.now())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(String identifier) {
        String key = accountKey(identifier);
        Instant now = Instant.now();

        jdbcTemplate.update("""
                INSERT INTO account_lockouts (account_key, failed_attempts, locked_until, updated_at)
                VALUES (?, 1, NULL, ?)
                ON CONFLICT (account_key) DO UPDATE SET
                    failed_attempts = account_lockouts.failed_attempts + 1,
                    updated_at = EXCLUDED.updated_at
                """, key, Timestamp.from(now));

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT failed_attempts FROM account_lockouts WHERE account_key = ?",
                Integer.class, key);

        if (attempts != null && attempts >= maxFailedAttempts) {
            jdbcTemplate.update(
                    "UPDATE account_lockouts SET locked_until = ? WHERE account_key = ?",
                    Timestamp.from(now.plus(Duration.ofMinutes(lockoutMinutes))), key);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerSuccessfulLogin(String identifier) {
        jdbcTemplate.update("DELETE FROM account_lockouts WHERE account_key = ?", accountKey(identifier));
    }

    @Scheduled(cron = "${app.account-lockout.cleanup-cron:0 27 * * * *}")
    public void deleteStaleCounters() {
        jdbcTemplate.update(
                "DELETE FROM account_lockouts WHERE (locked_until IS NULL OR locked_until < CURRENT_TIMESTAMP) "
                        + "AND updated_at < CURRENT_TIMESTAMP - INTERVAL '7 days'");
    }

    private String accountKey(String identifier) {
        String safe = identifier == null || identifier.isBlank()
                ? "unknown"
                : identifier.trim().toLowerCase(Locale.ROOT);
        return SecurityTokenUtils.hash("account-lockout:" + safe);
    }
}
