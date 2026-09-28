package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.MarcaProductoRequest;
import veterinaria.vargasvet.dto.response.MarcaProductoResponse;

import java.util.List;

public interface MarcaProductoService {
    Page<MarcaProductoResponse> listar(Integer companyId, String search, Boolean activo, int page, int size);
    List<MarcaProductoResponse> listarActivas(Integer companyId);
    MarcaProductoResponse crear(MarcaProductoRequest request);
    MarcaProductoResponse actualizar(Long id, MarcaProductoRequest request);
    MarcaProductoResponse toggleActivo(Long id);
    void eliminar(Long id);
}
