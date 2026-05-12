package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.ApoderadoListResponse;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface ApoderadoService {
    UserProfileDTO registerApoderado(ApoderadoRequest dto);
    UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto);
    void cambiarEstado(Long id, Boolean nuevoEstado);
    void eliminar(Long id);
    Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size);
    ApoderadoRequest findById(Long id);
}
