package veterinaria.vargasvet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.dto.ApiResponse;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogs(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp,desc") String sort,
            @RequestParam(required = false, defaultValue = "false") boolean initialLoad) {

        // Multi-tenant Security Check:
        // Si no es Super Admin, forzamos que filtre únicamente por la empresa de su sesión
        if (!SecurityUtils.isSuperAdmin()) {
            companyId = SecurityUtils.getCurrentCompanyId();
            if (companyId == null) {
                return ResponseEntity.status(403).body(new ApiResponse<>(false, "Acceso denegado: No tienes un ID de empresa asignado", null));
            }
        }

        // Configurar paginación y ordenamiento
        String[] sortParts = sort.split(",");
        Sort sortOrder = Sort.by(sortParts[0]);
        if (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) {
            sortOrder = sortOrder.descending();
        } else {
            sortOrder = sortOrder.ascending();
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Práctica de Alta Seguridad: Auditar al Auditor.
        // Solo se audita cuando el frontend envía initialLoad=true (únicamente en ngOnInit).
        // Cambiar de página o aplicar filtros nunca envían este flag.
        if (initialLoad) {
            String currentUserEmail = SecurityUtils.getCurrentUserEmail();
            Integer currentCompId = SecurityUtils.getCurrentCompanyId();
            String userRole = SecurityUtils.isSuperAdmin() ? "ROLE_SUPER_ADMIN" : "ROLE_ADMIN";
            String details = String.format("El usuario %s consultó el historial de auditoría.", currentUserEmail);

            auditLogService.log(
                currentUserEmail,
                userRole,
                currentCompId,
                null,
                "CONSULTA_AUDITORIA",
                "Seguridad",
                details,
                null
            );
        }

        Page<AuditLog> logs = auditLogService.getLogs(companyId, userEmail, action, module, startDate, endDate, pageable);

        return ResponseEntity.ok(new ApiResponse<>(true, "Logs de auditoría obtenidos correctamente", logs));
    }
}
