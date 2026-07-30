package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.time.LocalDate;

public interface ReportesClinicosService {
    ReportesClinicosDTO obtenerReportes(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta,
                                        Long veterinarioId, EspecieMascota especie);
}
