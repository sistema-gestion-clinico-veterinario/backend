package veterinaria.vargasvet.modules.pagos.service;

import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoResponse;

public interface PagoService {
    PagoResponse registrar(PagoRequest request);
    PagoResponse obtenerPorCita(Long citaId);
}
