package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;

public interface MascotaService {
    MascotaResponse registerMascota(MascotaRequest request);
    MascotaResponse updateMascota(Long id, MascotaRequest request);
    void cambiarEstado(Long id, veterinaria.vargasvet.dto.request.EstadoMascotaRequest request);
}
