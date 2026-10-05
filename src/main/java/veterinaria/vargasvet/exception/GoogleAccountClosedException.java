package veterinaria.vargasvet.exception;

public class GoogleAccountClosedException extends RuntimeException {
    public GoogleAccountClosedException() {
        super("Cerraste tu cuenta en esta veterinaria");
    }
}
