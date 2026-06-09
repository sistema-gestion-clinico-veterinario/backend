package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.PrescripcionService;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
public class PrescripcionController {

    private final PrescripcionService prescripcionService;
    private final AccesoValidator accesoValidator;

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<Page<PrescripcionResumenResponse>>> buscar(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String nombreMascota,
            @RequestParam(required = false) String numeroMicrochip,
            @RequestParam(required = false) String numeroDocumentoApoderado,
            @RequestParam(required = false) String numeroDocumentoEmpleado,
            @RequestParam(required = false) String numeroHc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas obtenidas",
                prescripcionService.buscar(query, companyId, nombreMascota, numeroMicrochip,
                        numeroDocumentoApoderado, numeroDocumentoEmpleado, numeroHc,
                        fechaDesde, fechaHasta, page, size)));
    }

    @PostMapping("/consultation/{consultationId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<PrescripcionResumenResponse>> crear(
            @PathVariable("consultationId") Long consultaId,
            @Valid @RequestBody PrescripcionRequest request) {
        accesoValidator.validarEscribir("VISTA_HISTORIAS");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Receta registrada exitosamente", prescripcionService.crear(consultaId, request)));
    }

    @GetMapping("/consultation/{consultationId}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_READ')")
    public ResponseEntity<ApiResponse<List<PrescripcionResumenResponse>>> listarPorConsulta(
            @PathVariable("consultationId") Long consultaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Recetas obtenidas", prescripcionService.listarPorConsulta(consultaId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<PrescripcionResumenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PrescripcionRequest request) {
        accesoValidator.validarModificar("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Receta actualizada exitosamente", prescripcionService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLINICAL_RECORD_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        accesoValidator.validarEliminar("VISTA_HISTORIAS");
        prescripcionService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Receta eliminada exitosamente", null));
    }
}
