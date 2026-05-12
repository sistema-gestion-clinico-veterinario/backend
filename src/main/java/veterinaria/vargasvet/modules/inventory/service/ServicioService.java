package veterinaria.vargasvet.modules.inventory.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.inventory.dto.ServicioRequest;
import veterinaria.vargasvet.modules.inventory.dto.ServicioResponse;

import java.util.List;

public interface ServicioService {
    Page<ServicioResponse> listar(Integer companyId, int page, int size);
    List<ServicioResponse> listarDisponibles(Integer companyId);
    ServicioResponse crear(ServicioRequest request);
    ServicioResponse actualizar(Long id, ServicioRequest request);
    void eliminar(Long id);
    ServicioResponse toggleDisponible(Long id);
}
