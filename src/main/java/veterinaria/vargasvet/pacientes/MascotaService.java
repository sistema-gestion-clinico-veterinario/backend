package veterinaria.vargasvet.pacientes;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.shared.EspecieMascota;
import veterinaria.vargasvet.pacientes.EstadoMascotaRequest;
import veterinaria.vargasvet.pacientes.MascotaRequest;
import veterinaria.vargasvet.pacientes.MascotaResponse;

public interface MascotaService {
    MascotaResponse registerMascota(MascotaRequest request);
    MascotaResponse updateMascota(Long id, MascotaRequest request);
    void cambiarEstado(Long id, EstadoMascotaRequest request);
    Page<MascotaResponse> listar(Integer companyId, String nombre, EspecieMascota especie, String nombrePropietario, int page, int size);
}
