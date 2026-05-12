package veterinaria.vargasvet.modules.users.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.users.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.modules.users.dto.response.ApoderadoListResponse;
import veterinaria.vargasvet.modules.users.dto.response.UserProfileDTO;

public interface ApoderadoService {
    UserProfileDTO registerApoderado(ApoderadoRequest dto);
    UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto);
    void cambiarEstado(Long id, Boolean nuevoEstado);
    Page<ApoderadoListResponse> listar(Integer companyId, String nombre, String numeroDocumento, int page, int size);
    ApoderadoRequest findById(Long id);
}
