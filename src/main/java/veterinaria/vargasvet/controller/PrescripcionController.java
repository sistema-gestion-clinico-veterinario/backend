package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.service.PrescripcionService;

import java.util.List;

@RestController
@RequestMapping("/prescripciones")
@RequiredArgsConstructor
public class PrescripcionController {

    private final PrescripcionService prescripcionService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<Page<PrescripcionResumenResponse>>> buscar(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas obtenidas",
                prescripcionService.buscar(query, companyId, page, size)));
    }

    @PostMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<PrescripcionResumenResponse>> crear(
            @PathVariable Long consultaId,
            @Valid @RequestBody PrescripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Receta registrada exitosamente", prescripcionService.crear(consultaId, request)));
    }

    @GetMapping("/consulta/{consultaId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<List<PrescripcionResumenResponse>>> listarPorConsulta(
            @PathVariable Long consultaId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas obtenidas", prescripcionService.listarPorConsulta(consultaId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<PrescripcionResumenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PrescripcionRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Receta actualizada exitosamente", prescripcionService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        prescripcionService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Receta eliminada exitosamente", null));
    }
}
