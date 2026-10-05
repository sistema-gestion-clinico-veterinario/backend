package veterinaria.vargasvet.exception;

public class GoogleAccountSuspendedException extends RuntimeException {
    public GoogleAccountSuspendedException() {
        super("Tu acceso a esta veterinaria está suspendido");
    }
}
