package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.ApoderadoRequest;
import veterinaria.vargasvet.dto.response.UserProfileDTO;

public interface ApoderadoService {
    UserProfileDTO registerApoderado(ApoderadoRequest dto);
    UserProfileDTO updateApoderado(Long id, ApoderadoRequest dto);
    void cambiarEstado(Long id, Boolean nuevoEstado);
}
