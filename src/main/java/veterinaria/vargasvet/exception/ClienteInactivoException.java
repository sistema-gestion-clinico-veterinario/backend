package veterinaria.vargasvet.exception;

import veterinaria.vargasvet.domain.enums.TipoInactividad;

public class ClienteInactivoException extends IllegalArgumentException {

    private final Long apoderadoId;
    private final TipoInactividad tipoInactividad;

    public ClienteInactivoException(String message, Long apoderadoId, TipoInactividad tipoInactividad) {
        super(message);
        this.apoderadoId = apoderadoId;
        this.tipoInactividad = tipoInactividad;
    }

    public Long getApoderadoId() {
        return apoderadoId;
    }

    public TipoInactividad getTipoInactividad() {
        return tipoInactividad;
    }
}
