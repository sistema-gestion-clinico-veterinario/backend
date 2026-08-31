package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
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
}
