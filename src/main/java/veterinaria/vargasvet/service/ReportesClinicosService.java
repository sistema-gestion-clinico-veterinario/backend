package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.dto.response.ReporteVentasProductosDTO;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.dto.request.ReporteVentasProductosFiltro;

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

    ReporteVentasProductosDTO obtenerVentasProductos(ReporteVentasProductosFiltro filtros);

    /** Compatibilidad para consumidores internos que todavía solo envían el periodo. */
    default ReporteVentasProductosDTO obtenerVentasProductos(Integer companyId, LocalDate fechaDesde, LocalDate fechaHasta) {
        return obtenerVentasProductos(ReporteVentasProductosFiltro.builder()
                .companyId(companyId).fechaDesde(fechaDesde).fechaHasta(fechaHasta).build());
    }
}
