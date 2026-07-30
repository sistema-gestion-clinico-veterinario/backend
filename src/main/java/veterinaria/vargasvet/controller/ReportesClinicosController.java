package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.ReportesClinicosDTO;
import veterinaria.vargasvet.service.ReportesClinicosService;

@RestController
@RequestMapping("/clinical-reports")
@RequiredArgsConstructor
public class ReportesClinicosController {

    private final ReportesClinicosService reportesClinicosService;

    @GetMapping
    public ResponseEntity<ApiResponse<ReportesClinicosDTO>> obtenerReportes(
            @RequestParam(required = false) Integer companyId) {
        ReportesClinicosDTO resultado = reportesClinicosService.obtenerReportes(companyId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reportes clínicos obtenidos con éxito", resultado));
    }
}
