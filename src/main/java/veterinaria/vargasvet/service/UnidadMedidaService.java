package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.UnidadMedidaRequest;
import veterinaria.vargasvet.dto.response.UnidadMedidaResponse;

import java.util.List;

public interface UnidadMedidaService {
    Page<UnidadMedidaResponse> listar(Integer companyId, String search, Boolean activo, int page, int size);
    List<UnidadMedidaResponse> listarActivas(Integer companyId);
    UnidadMedidaResponse crear(UnidadMedidaRequest request);
    UnidadMedidaResponse actualizar(Long id, UnidadMedidaRequest request);
    void eliminar(Long id);
    UnidadMedidaResponse toggleActivo(Long id);
}
