package veterinaria.vargasvet.modules.citas.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.citas.domain.enums.EstadoCita;
import veterinaria.vargasvet.modules.citas.dto.CitaRequest;
import veterinaria.vargasvet.modules.citas.dto.CitaResponse;

import java.time.LocalDate;

public interface CitaService {
    CitaResponse createCita(CitaRequest request);
    Long iniciarAtencion(Long id);
    Page<CitaResponse> listar(Integer companyId, LocalDate fecha, EstadoCita estado, Long veterinarioId, int page, int size);
    void cancelarCita(Long id, String motivo);
    void eliminarCita(Long id);
    CitaResponse actualizarCita(Long id, CitaRequest request);
    CitaResponse reprogramarCita(Long id, CitaRequest request);
}
