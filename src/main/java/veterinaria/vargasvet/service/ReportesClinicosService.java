package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.time.LocalDate;

public interface ReportesClinicosService {
    ReportesClinicosDTO obtenerReportes(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta,
                                        Long veterinarioId, EspecieMascota especie);

    /** Solo para administración de plataforma: un resumen por empresa, sin mezclar sus cifras. */
    ReportesComparativoEmpresasDTO obtenerComparativoEmpresas(LocalDate fechaDesde, LocalDate fechaHasta,
                                                               EspecieMascota especie);

    /** Paginado desde la base de datos: evita cargar todas las mascotas activas de la empresa
     * en memoria solo para este panel. */
    PacientesInactivosPageDTO obtenerPacientesInactivos(Integer companyId, int page, int size);
}
