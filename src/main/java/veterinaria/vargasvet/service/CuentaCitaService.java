package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.DetalleCuentaRequest;
import veterinaria.vargasvet.dto.response.CuentaCitaResponse;

public interface CuentaCitaService {
    Page<CuentaCitaResponse> listarPendientes(Integer companyId, int page, int size);
    CuentaCitaResponse obtener(Long citaId);
    CuentaCitaResponse agregarDetalle(Long citaId, DetalleCuentaRequest request);
    CuentaCitaResponse actualizarDetalle(Long citaId, Long detalleId, DetalleCuentaRequest request);
    CuentaCitaResponse eliminarDetalle(Long citaId, Long detalleId);
}
