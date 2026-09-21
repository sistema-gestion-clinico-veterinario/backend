package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.DiagnosticoRequest;
import veterinaria.vargasvet.dto.response.DiagnosticoResumenResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.DiagnosticoService;

import java.util.List;

@RestController
@RequestMapping("/diagnoses")
@RequiredArgsConstructor
public class DiagnosticoController {

    private final DiagnosticoService diagnosticoService;
    private final AccesoValidator accesoValidator;

    @PostMapping("/consultation/{consultationId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'ESCRIBIR')")
    public ResponseEntity<ApiResponse<DiagnosticoResumenResponse>> crear(
            @PathVariable("consultationId") Long consultaId,
            @Valid @RequestBody DiagnosticoRequest request) {
        accesoValidator.validarEscribir("VISTA_HISTORIAS");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Diagnóstico registrado exitosamente", diagnosticoService.crear(consultaId, request)));
    }

    @GetMapping("/consultation/{consultationId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<DiagnosticoResumenResponse>>> listarPorConsulta(
            @PathVariable("consultationId") Long consultaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Diagnósticos obtenidos", diagnosticoService.listarPorConsulta(consultaId)));
    }

    @GetMapping("/pets/{petId}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'LEER')")
    public ResponseEntity<ApiResponse<List<DiagnosticoResumenResponse>>> listarPorMascota(@PathVariable("petId") Long mascotaId) {
        accesoValidator.validarLeer("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Diagnósticos de la mascota obtenidos", diagnosticoService.listarPorMascota(mascotaId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<DiagnosticoResumenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosticoRequest request) {
        accesoValidator.validarModificar("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Diagnóstico actualizado exitosamente", diagnosticoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'MODIFICAR')")
    public ResponseEntity<ApiResponse<DiagnosticoResumenResponse>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoDiagnostico estado) {
        accesoValidator.validarModificar("VISTA_HISTORIAS");
        return ResponseEntity.ok(new ApiResponse<>(true, "Estado del diagnóstico actualizado", diagnosticoService.cambiarEstado(id, estado)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accesoValidator.can('VISTA_HISTORIAS', 'ELIMINAR')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        accesoValidator.validarEliminar("VISTA_HISTORIAS");
        diagnosticoService.eliminar(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Diagnóstico eliminado exitosamente", null));
    }
}
