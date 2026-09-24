package veterinaria.vargasvet.exception;

public class GoogleEmailMismatchException extends RuntimeException {
    public GoogleEmailMismatchException() {
        super("El correo de la cuenta de Google no coincide con el correo de la invitación");
    }
}
