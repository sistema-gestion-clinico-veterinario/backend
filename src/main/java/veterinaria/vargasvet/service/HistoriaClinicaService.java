package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.dto.response.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaListResponse;

import java.time.LocalDate;

public interface HistoriaClinicaService {
    Page<HistoriaClinicaListResponse> buscar(String numeroHc, String nombrePaciente, String nombrePropietario,
                                             LocalDate fechaDesde, LocalDate fechaHasta,
                                             Integer companyId, int page, int size);

    HistoriaClinicaDetalleResponse getDetalle(Long id);

    HistoriaClinicaDetalleResponse getPorMascota(Long mascotaId);
}
