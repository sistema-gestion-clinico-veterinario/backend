package veterinaria.vargasvet.modules.clinical.service;

import veterinaria.vargasvet.modules.clinical.dto.PrescripcionRequest;
import veterinaria.vargasvet.modules.clinical.dto.PrescripcionResumenResponse;

import java.util.List;

public interface PrescripcionService {
    PrescripcionResumenResponse crear(Long consultaId, PrescripcionRequest request);
    List<PrescripcionResumenResponse> listarPorConsulta(Long consultaId);
    PrescripcionResumenResponse actualizar(Long id, PrescripcionRequest request);
    void eliminar(Long id);
}
