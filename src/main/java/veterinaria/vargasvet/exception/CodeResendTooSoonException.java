package veterinaria.vargasvet.exception;

public class CodeResendTooSoonException extends RuntimeException {

    private final long retryAfterSeconds;

    public CodeResendTooSoonException(long retryAfterSeconds) {
        super("Espera " + retryAfterSeconds + " segundos antes de pedir otro código.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
