package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaConteoResponse;
import veterinaria.vargasvet.dto.response.ProductoResponse;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponse> buscar(Integer companyId, String search, Long categoriaId, Boolean activo, int page, int size);
    ProductoResponse obtener(Long id);
    List<ProductoResponse> listarActivos(Integer companyId);
    List<CategoriaConteoResponse> conteoPorCategoria(Integer companyId);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
    ProductoResponse toggleActivo(Long id);
}
