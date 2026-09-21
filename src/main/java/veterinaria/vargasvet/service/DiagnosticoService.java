package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.dto.request.DiagnosticoRequest;
import veterinaria.vargasvet.dto.response.DiagnosticoResumenResponse;

import java.util.List;

public interface DiagnosticoService {
    DiagnosticoResumenResponse crear(Long consultaId, DiagnosticoRequest request);
    List<DiagnosticoResumenResponse> listarPorConsulta(Long consultaId);
    List<DiagnosticoResumenResponse> listarPorMascota(Long mascotaId);
    DiagnosticoResumenResponse actualizar(Long id, DiagnosticoRequest request);
    DiagnosticoResumenResponse cambiarEstado(Long id, EstadoDiagnostico nuevoEstado);
    void eliminar(Long id);
}
