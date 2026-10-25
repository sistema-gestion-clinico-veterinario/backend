package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.dto.response.BackupExecutionResponse;
import veterinaria.vargasvet.dto.response.BackupSummaryResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.BackupExecutionService;
import veterinaria.vargasvet.service.BackupSummaryService;

@RestController
@RequestMapping("/admin/backups")
@RequiredArgsConstructor
public class BackupExecutionController {

    private final BackupExecutionService service;
    private final BackupSummaryService summaryService;

    @GetMapping
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_RESPALDOS', 'LEER')")
    public ResponseEntity<ApiResponse<Page<BackupExecutionResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Solo la administración de plataforma puede consultar respaldos");
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Ejecuciones de respaldo obtenidas", service.list(page, size)));
    }

    @GetMapping("/summary")
    @PreAuthorize("@accesoValidator.hasPurpose('PLATFORM_ADMIN') and @accesoValidator.can('VISTA_RESPALDOS', 'LEER')")
    public ResponseEntity<ApiResponse<BackupSummaryResponse>> summary() {
        if (!SecurityUtils.isSuperAdmin()) {
            throw new AccessDeniedException("Solo la administración de plataforma puede consultar respaldos");
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumen de respaldos obtenido", summaryService.summary()));
    }
}
