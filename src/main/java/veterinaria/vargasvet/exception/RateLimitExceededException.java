package veterinaria.vargasvet.exception;

import java.time.Duration;

public class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitExceededException(Duration retryAfter) {
        super("Demasiados intentos. Espere unos minutos antes de volver a intentarlo.");
        if (retryAfter == null || retryAfter.isZero() || retryAfter.isNegative()) {
            throw new IllegalArgumentException("El tiempo de reintento debe ser positivo");
        }
        this.retryAfterSeconds = Math.max(1, retryAfter.toSeconds());
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
