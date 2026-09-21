package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.PacientesInactivosPageDTO;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.dto.response.ReportesComparativoEmpresasDTO;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.service.ReportesClinicosService;

import java.time.LocalDate;

@RestController
@RequestMapping("/clinical-reports")
@RequiredArgsConstructor
@PreAuthorize("@accesoValidator.can('VISTA_REPORTES', 'LEER')")
public class ReportesClinicosController {

    private final ReportesClinicosService reportesClinicosService;

    @GetMapping
    public ResponseEntity<ApiResponse<ReportesClinicosDTO>> obtenerReportes(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) EspecieMascota especie) {
        ReportesClinicosDTO resultado = reportesClinicosService.obtenerReportes(
                companyId, fechaDesde, fechaHasta, veterinarioId, especie);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reportes clínicos obtenidos con éxito", resultado));
    }

    /**
     * Comparativo por empresa, sin mezclar sus cifras. Restringido a administración de
     * plataforma: el permiso de vista de reportes de la clase (VISTA_REPORTES) puede estar
     * concedido a roles de una sola empresa, que nunca deben ver datos de otras.
     */
    @GetMapping("/comparison")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<ReportesComparativoEmpresasDTO>> obtenerComparativoEmpresas(
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(required = false) EspecieMascota especie) {
        ReportesComparativoEmpresasDTO resultado = reportesClinicosService.obtenerComparativoEmpresas(
                fechaDesde, fechaHasta, especie);
        return ResponseEntity.ok(new ApiResponse<>(true, "Comparativo de empresas obtenido con éxito", resultado));
    }

    /** Paginado desde la base de datos (ver MascotaRepository.findInactivasByCompanyId): la
     * tabla "Pacientes sin visitar" se pide por página en vez de traer todas las mascotas
     * inactivas de la empresa en cada carga del panel de reportes. */
    @GetMapping("/inactive-patients")
    public ResponseEntity<ApiResponse<PacientesInactivosPageDTO>> obtenerPacientesInactivos(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PacientesInactivosPageDTO resultado = reportesClinicosService.obtenerPacientesInactivos(companyId, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pacientes inactivos obtenidos con éxito", resultado));
    }
}
