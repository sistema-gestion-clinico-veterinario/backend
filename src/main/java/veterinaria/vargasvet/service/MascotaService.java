package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.request.EstadoMascotaRequest;
import veterinaria.vargasvet.dto.request.MascotaRequest;
import veterinaria.vargasvet.dto.response.MascotaResponse;

public interface MascotaService {
    MascotaResponse registerMascota(MascotaRequest request);
    MascotaResponse updateMascota(Long id, MascotaRequest request);
    void cambiarEstado(Long id, EstadoMascotaRequest request);
    Page<MascotaResponse> listar(Integer companyId, String nombre, EspecieMascota especie, String nombrePropietario, int page, int size);
}
