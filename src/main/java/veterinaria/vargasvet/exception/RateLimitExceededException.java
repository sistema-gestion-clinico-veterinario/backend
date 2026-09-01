package veterinaria.vargasvet.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Demasiados intentos. Espere unos minutos antes de volver a intentarlo.");
    }
}
