package veterinaria.vargasvet.pagos;

import veterinaria.vargasvet.pagos.PagoRequest;
import veterinaria.vargasvet.pagos.PagoResponse;

public interface PagoService {
    PagoResponse registrar(PagoRequest request);
    PagoResponse obtenerPorCita(Long citaId);
}
