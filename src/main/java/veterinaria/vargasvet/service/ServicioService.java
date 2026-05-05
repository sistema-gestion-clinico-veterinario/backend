package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.ServicioRequest;
import veterinaria.vargasvet.dto.response.ServicioResponse;

import java.util.List;

public interface ServicioService {
    Page<ServicioResponse> listar(Integer companyId, int page, int size);
    List<ServicioResponse> listarDisponibles(Integer companyId);
    ServicioResponse crear(ServicioRequest request);
    ServicioResponse actualizar(Long id, ServicioRequest request);
    void eliminar(Long id);
    ServicioResponse toggleDisponible(Long id);
}
