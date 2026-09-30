package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.MascotaRelacionRequest;
import veterinaria.vargasvet.dto.response.MascotaRelacionResponse;

import java.util.List;

public interface MascotaRelacionService {
    List<MascotaRelacionResponse> listar(String mascotaUuid);
    MascotaRelacionResponse crear(String mascotaUuid, MascotaRelacionRequest request);
    MascotaRelacionResponse actualizar(String mascotaUuid, String relacionUuid, MascotaRelacionRequest request);
    void revocar(String mascotaUuid, String relacionUuid);
    void asegurarPropietarioPrincipal(Long mascotaId);
}
