package veterinaria.vargasvet.clinica;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.clinica.PrescripcionRequest;
import veterinaria.vargasvet.clinica.PrescripcionResumenResponse;

import java.util.List;

public interface PrescripcionService {
    PrescripcionResumenResponse crear(Long consultaId, PrescripcionRequest request);
    List<PrescripcionResumenResponse> listarPorConsulta(Long consultaId);
    PrescripcionResumenResponse actualizar(Long id, PrescripcionRequest request);
    void eliminar(Long id);
    Page<PrescripcionResumenResponse> buscar(String query, Integer companyId, int page, int size);
}
