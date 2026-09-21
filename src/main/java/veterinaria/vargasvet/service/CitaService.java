package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.request.CitaReprogramacionRequest;
import veterinaria.vargasvet.dto.request.CitaReasignacionVeterinarioRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.dto.response.AgendaCountersResponse;
import veterinaria.vargasvet.dto.response.RecordatorioWhatsAppResponse;

import java.time.LocalDate;
import java.util.List;

public interface CitaService {
    CitaResponse createCita(CitaRequest request);
    Long iniciarAtencion(Long id);
    boolean requiereConsultaClinica(Long id);
    CitaResponse finalizarServicio(Long id, String notas);
    Page<CitaResponse> listar(
            Integer companyId,
            LocalDate fecha,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoCita estado,
            Long veterinarioId,
            int page,
            int size);
    AgendaCountersResponse obtenerContadores(
            Integer companyId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long veterinarioId);
    void cancelarCita(Long id, String motivo);
    CitaResponse marcarNoAsistio(Long id);
    CitaResponse marcarLlegada(Long id);
    void eliminarCita(Long id);
    CitaResponse actualizarCita(Long id, CitaRequest request);
    CitaResponse reprogramarCita(Long id, CitaRequest request);
    CitaResponse reprogramarCita(Long id, CitaReprogramacionRequest request);
    CitaResponse reasignarVeterinario(Long id, CitaReasignacionVeterinarioRequest request);
    List<CitaResponse> listarCitasVigentesPorEmpleado(Long empleadoId);
    List<CitaResponse> listarCitasVigentesPorApoderado(Long apoderadoId);
    List<String> getAdminDisponibilidad(Long empleadoId, String fecha, Long servicioId, Boolean esEmergencia, Long excludeCitaId);
    List<CitaResponse> getServiciosNoMedicos(Long mascotaId);
    List<RecordatorioWhatsAppResponse> listarRecordatoriosWhatsApp(Integer companyId);
}
