package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.ProductoRequest;
import veterinaria.vargasvet.dto.response.ProductoResponse;

import java.util.List;

public interface ProductoService {
    Page<ProductoResponse> listar(Integer companyId, int page, int size);
    List<ProductoResponse> listarActivos(Integer companyId);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
    ProductoResponse toggleActivo(Long id);
}
