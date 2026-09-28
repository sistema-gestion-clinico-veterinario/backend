package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.CategoriaConteoResponse;
import veterinaria.vargasvet.dto.response.ProductoResponse;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponse> buscar(Integer companyId, String search, Long categoriaId, Boolean activo, int page, int size);
    ProductoResponse obtener(String sku, Integer companyId);
    List<ProductoResponse> listarActivos(Integer companyId);
    List<CategoriaConteoResponse> conteoPorCategoria(Integer companyId);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(String sku, ProductoRequest request);
    void eliminar(String sku, Integer companyId);
    ProductoResponse toggleActivo(String sku, Integer companyId);
}
