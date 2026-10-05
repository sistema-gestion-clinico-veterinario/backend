package veterinaria.vargasvet.exception;

public class InvalidVerificationCodeException extends IllegalArgumentException {
    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
