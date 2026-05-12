package veterinaria.vargasvet.modules.clinical.service;

import org.springframework.data.domain.Page;
import veterinaria.vargasvet.modules.clinical.dto.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.modules.clinical.dto.HistoriaClinicaListResponse;

import java.time.LocalDate;

public interface HistoriaClinicaService {
    Page<HistoriaClinicaListResponse> buscar(String numeroHc, String nombrePaciente, String nombrePropietario,
                                             LocalDate fechaDesde, LocalDate fechaHasta, int page, int size);

    HistoriaClinicaDetalleResponse getDetalle(Long id);

    HistoriaClinicaDetalleResponse getPorMascota(Long mascotaId);
}
