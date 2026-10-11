package veterinaria.vargasvet.dto.response;

public record CancelacionDatosElegibilidad(
        boolean puedeSolicitar,
        String motivoBloqueo,
        boolean requierePassword,
        SolicitudCancelacionResponse solicitudVigente) {
}
