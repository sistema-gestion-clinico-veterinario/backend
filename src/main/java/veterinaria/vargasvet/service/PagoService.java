package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.PagoRequest;
import veterinaria.vargasvet.dto.response.PagoListResponse;
import veterinaria.vargasvet.dto.response.PagoResponse;

public interface PagoService {
    PagoResponse registrar(PagoRequest request);
    PagoResponse obtenerPorCita(Long citaId);
    Page<PagoListResponse> listarTodos(int page, int size, Integer companyId);
    Page<PagoListResponse> listarMisPagos(int page, int size);
    Page<PagoListResponse> listarHistorialPorEmpresa(int page, int size, Integer companyId);
}
