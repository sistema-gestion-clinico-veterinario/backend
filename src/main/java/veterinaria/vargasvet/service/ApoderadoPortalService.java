package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.*;
import java.util.List;

public interface ApoderadoPortalService {
    ApoderadoPerfilResponse getPerfil();
    List<MascotaResponse> getMascotas();
    org.springframework.data.domain.Page<MascotaResponse> getMascotasPaginated(String nombre, veterinaria.vargasvet.domain.enums.EspecieMascota especie, Boolean activo, org.springframework.data.domain.Pageable pageable);
    HistoriaClinicaDetalleResponse getHistoriaMascota(Long mascotaId);
    List<CitaResponse> getCitas(Long mascotaId);
    List<PrescripcionResumenResponse> getRecetas();
    List<ServicioResponse> getServicios();
    List<EmpleadoListResponse> getEmpleados(Long servicioId);
    List<HorarioEmpleadoResponse> getHorarioEmpleado(Long empleadoId);
    List<String> getDisponibilidad(Long empleadoId, String fecha, Long servicioId);
    CitaResponse createPortalCita(CitaRequest request);
    CitaResponse updatePortalCita(Long id, CitaRequest request);
    CitaResponse reschedulePortalCita(Long id, CitaRequest request);
    void cancelPortalCita(Long id, String motivo);
}

