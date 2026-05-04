package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;

public interface CitaService {
    CitaResponse createCita(CitaRequest request);
    Long iniciarAtencion(Long id);
}
