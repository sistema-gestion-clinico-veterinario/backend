package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.enums.EstadoTratamiento;
import veterinaria.vargasvet.dto.request.TratamientoRequest;
import veterinaria.vargasvet.dto.response.TratamientoResumenResponse;

import java.util.List;

public interface TratamientoService {
    TratamientoResumenResponse crear(Long consultaId, TratamientoRequest request);
    List<TratamientoResumenResponse> listarPorConsulta(Long consultaId);
    List<TratamientoResumenResponse> listarPorMascota(Long mascotaId);
    TratamientoResumenResponse actualizar(Long id, TratamientoRequest request);
    TratamientoResumenResponse cambiarEstado(Long id, EstadoTratamiento nuevoEstado);
    void eliminar(Long id);
}
