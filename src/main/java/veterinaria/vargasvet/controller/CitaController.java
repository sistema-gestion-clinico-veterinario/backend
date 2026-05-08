package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.service.CitaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Page<CitaResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CitaResponse> resultado = citaService.listar(companyId, fecha, estado, veterinarioId, page, size);
        String mensaje = resultado.isEmpty() ? "No se encontraron citas" : "Citas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<CitaResponse>> createCita(@Valid @RequestBody CitaRequest request) {
        CitaResponse response = citaService.createCita(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Cita programada exitosamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<CitaResponse>> actualizarCita(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        CitaResponse response = citaService.actualizarCita(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita actualizada exitosamente", response));
    }

    @PutMapping("/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<CitaResponse>> reprogramarCita(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        CitaResponse response = citaService.reprogramarCita(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita reprogramada exitosamente", response));
    }

    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO')")
    public ResponseEntity<ApiResponse<Long>> iniciarAtencion(@PathVariable Long id) {
        Long consultaId = citaService.iniciarAtencion(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Atención iniciada con éxito", consultaId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarCita(@PathVariable Long id) {
        citaService.eliminarCita(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita eliminada con éxito", null));
    }

    @DeleteMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<Void>> cancelarCita(@PathVariable Long id, @RequestParam String motivo) {
        citaService.cancelarCita(id, motivo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita cancelada con éxito", null));
    }
}
