package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;

public interface ReportesClinicosService {
    ReportesClinicosDTO obtenerReportes(Integer companyId);
}
