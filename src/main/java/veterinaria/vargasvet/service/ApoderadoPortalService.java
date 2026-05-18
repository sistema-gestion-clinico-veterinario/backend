package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.*;
import java.util.List;

public interface ApoderadoPortalService {
    ApoderadoPerfilResponse getPerfil();
    List<MascotaResponse> getMascotas();
    HistoriaClinicaDetalleResponse getHistoriaMascota(Long mascotaId);
    List<CitaResponse> getCitas(Long mascotaId);
    List<PrescripcionResumenResponse> getRecetas();
    List<ServicioResponse> getServicios();
    List<EmpleadoListResponse> getEmpleados(Long servicioId);
    List<String> getDisponibilidad(Long empleadoId, String fecha, Long servicioId);
    CitaResponse createPortalCita(CitaRequest request);
    CitaResponse updatePortalCita(Long id, CitaRequest request);
    CitaResponse reschedulePortalCita(Long id, CitaRequest request);
}

