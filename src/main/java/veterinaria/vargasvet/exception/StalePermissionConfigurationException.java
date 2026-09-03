package veterinaria.vargasvet.exception;

public class StalePermissionConfigurationException extends RuntimeException {
    public StalePermissionConfigurationException() {
        super("Los permisos de este rol fueron modificados por otro usuario. Recarga el rol antes de volver a guardar.");
    }
}
