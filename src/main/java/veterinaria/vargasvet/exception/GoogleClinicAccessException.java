package veterinaria.vargasvet.exception;

public class GoogleClinicAccessException extends RuntimeException {
    public GoogleClinicAccessException() {
        super("La cuenta de Google no está registrada en esta veterinaria");
    }
}
