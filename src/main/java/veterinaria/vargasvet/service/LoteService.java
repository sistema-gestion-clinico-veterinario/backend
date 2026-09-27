package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.LoteRequest;
import veterinaria.vargasvet.dto.response.AlertaVencimientoResponse;
import veterinaria.vargasvet.dto.response.LoteResponse;

import java.util.List;

public interface LoteService {
    Page<LoteResponse> listar(Integer companyId, String search, int page, int size);
    Page<LoteResponse> listarPorProducto(Long productoId, int page, int size);
    List<LoteResponse> listarActivosPorProducto(Long productoId);
    LoteResponse crear(LoteRequest request);
    LoteResponse actualizar(Long id, LoteRequest request);
    void eliminar(Long id);
    LoteResponse toggleActivo(Long id);
    AlertaVencimientoResponse alertasVencimiento(Integer companyId, int diasPorVencer);
}
