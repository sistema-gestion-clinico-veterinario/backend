package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.CitaRequest;
import veterinaria.vargasvet.dto.response.CitaResponse;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.service.CitaService;
import veterinaria.vargasvet.service.AuditLogService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;
    private final AuditLogService auditLogService;
    private final AccesoValidator accesoValidator;

    @GetMapping("/disponibilidad")
    public ResponseEntity<ApiResponse<List<String>>> getAdminDisponibilidad(
            @RequestParam Long empleadoId,
            @RequestParam String fecha,
            @RequestParam Long servicioId) {
        accesoValidator.validarLeer("VISTA_CITAS_AGENDA");
        List<String> slots = citaService.getAdminDisponibilidad(empleadoId, fecha, servicioId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Disponibilidad recuperada con éxito", slots));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CitaResponse>>> listar(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        accesoValidator.validarLeer("VISTA_CITAS_AGENDA");
        Page<CitaResponse> resultado = citaService.listar(companyId, fecha, estado, veterinarioId, page, size);
        auditLogService.log(companyId, "CONSULTAR_CITAS", "Citas", "Consultó la lista de citas.");
        String mensaje = resultado.isEmpty() ? "No se encontraron citas" : "Citas recuperadas con éxito";
        return ResponseEntity.ok(new ApiResponse<>(true, mensaje, resultado));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CitaResponse>> createCita(@Valid @RequestBody CitaRequest request) {
        accesoValidator.validarEscribir("VISTA_CITAS_AGENDA");
        CitaResponse response = citaService.createCita(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Cita programada exitosamente", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CitaResponse>> actualizarCita(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        accesoValidator.validarModificar("VISTA_CITAS_AGENDA");
        CitaResponse response = citaService.actualizarCita(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita actualizada exitosamente", response));
    }

    @PutMapping("/{id}/reprogramar")
    public ResponseEntity<ApiResponse<CitaResponse>> reprogramarCita(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        accesoValidator.validarModificar("VISTA_CITAS_AGENDA");
        CitaResponse response = citaService.reprogramarCita(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita reprogramada exitosamente", response));
    }

    @PatchMapping("/{id}/iniciar")
    public ResponseEntity<ApiResponse<Long>> iniciarAtencion(@PathVariable Long id) {
        accesoValidator.validarModificar("VISTA_CITAS_AGENDA");
        Long consultaId = citaService.iniciarAtencion(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Atención iniciada con éxito", consultaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminarCita(@PathVariable Long id) {
        accesoValidator.validarEliminar("VISTA_CITAS_AGENDA");
        citaService.eliminarCita(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita eliminada con éxito", null));
    }

    @DeleteMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<Void>> cancelarCita(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        accesoValidator.validarModificar("VISTA_CITAS_AGENDA");
        String finalMotivo = (motivo == null || motivo.isBlank()) ? "Cancelado por el usuario" : motivo;
        citaService.cancelarCita(id, finalMotivo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cita cancelada con éxito", null));
    }
}
