package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.CategoriaProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaProductoResponse;

import java.util.List;

public interface CategoriaProductoService {
    Page<CategoriaProductoResponse> listar(Integer companyId, int page, int size);
    List<CategoriaProductoResponse> listarActivas(Integer companyId);
    CategoriaProductoResponse crear(CategoriaProductoRequest request);
    CategoriaProductoResponse actualizar(Long id, CategoriaProductoRequest request);
    void eliminar(Long id);
    CategoriaProductoResponse toggleActivo(Long id);
}
