package veterinaria.vargasvet.exception;

public class AutorizacionIaRequeridaException extends RuntimeException {

    private final Long apoderadoId;

    public AutorizacionIaRequeridaException(Long apoderadoId) {
        super("El titular no autorizó el uso de inteligencia artificial con los datos clínicos de esta mascota.");
        this.apoderadoId = apoderadoId;
    }

    public Long getApoderadoId() {
        return apoderadoId;
    }
}
