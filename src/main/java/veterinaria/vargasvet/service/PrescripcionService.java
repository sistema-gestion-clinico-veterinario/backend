package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;

import java.util.List;

public interface PrescripcionService {
    PrescripcionResumenResponse crear(Long consultaId, PrescripcionRequest request);
    List<PrescripcionResumenResponse> listarPorConsulta(Long consultaId);
    PrescripcionResumenResponse actualizar(Long id, PrescripcionRequest request);
    void eliminar(Long id);
}
