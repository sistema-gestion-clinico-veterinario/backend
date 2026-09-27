package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.AjusteStockRequest;
import veterinaria.vargasvet.dto.response.AjusteStockResponse;

public interface AjusteStockService {
    AjusteStockResponse ajustar(AjusteStockRequest request);
    Page<AjusteStockResponse> listarPorProducto(Long productoId, int page, int size);
}
