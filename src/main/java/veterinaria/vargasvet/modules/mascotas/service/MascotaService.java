package veterinaria.vargasvet.modules.mascotas.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.mascotas.domain.enums.EspecieMascota;
import veterinaria.vargasvet.modules.mascotas.dto.EstadoMascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaRequest;
import veterinaria.vargasvet.modules.mascotas.dto.MascotaResponse;

public interface MascotaService {
    MascotaResponse registerMascota(MascotaRequest request);
    MascotaResponse updateMascota(Long id, MascotaRequest request);
    void cambiarEstado(Long id, EstadoMascotaRequest request);
    Page<MascotaResponse> listar(Integer companyId, String nombre, EspecieMascota especie, String nombrePropietario, int page, int size);
}
