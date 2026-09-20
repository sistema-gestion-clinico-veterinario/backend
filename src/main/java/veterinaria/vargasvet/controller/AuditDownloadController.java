package veterinaria.vargasvet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.enums.TipoDescarga;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.request.RegistrarDescargaRequest;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;

/**
 * Registra en el historial de auditoría las descargas/impresiones que el navegador genera
 * completamente en el cliente (PDF/Excel de horarios, cartillas, recetas...) y que por eso
 * nunca pasan por un endpoint de negocio que ya deje rastro por sí mismo.
 *
 * El módulo y la acción vienen fijados por {@link TipoDescarga} en el backend, nunca como texto
 * libre del cliente, para que el registro de auditoría no pueda falsificarse.
 */
@RestController
@RequestMapping("/audit-logs/downloads")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AuditDownloadController {

    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registrarDescarga(@Valid @RequestBody RegistrarDescargaRequest request) {
        TipoDescarga tipo = request.getTipo();
        String email = SecurityUtils.getCurrentUserEmail();

        String detalle = "El usuario " + email + " " + tipo.getDescripcion();
        if (request.getReferencia() != null && !request.getReferencia().isBlank()) {
            detalle += " (" + request.getReferencia().trim() + ")";
        }
        detalle += ".";

        auditLogService.log(tipo.getAccion(), tipo.getModulo(), detalle);

        return ResponseEntity.ok(new ApiResponse<>(true, "Descarga registrada", null));
    }
}
