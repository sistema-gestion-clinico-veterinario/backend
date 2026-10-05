package veterinaria.vargasvet.exception;

public class GoogleAccountDeactivatedException extends RuntimeException {
    public GoogleAccountDeactivatedException() {
        super("Tu acceso a esta veterinaria ya no está activo");
    }
}
