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
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.util.AppClock;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("@accesoValidator.can('VISTA_AUDITORIA_ADMIN', 'LEER')")
public class AuditLogController {

    private static final Set<String> CAMPOS_ORDENABLES =
            Set.of("timestamp", "userEmail", "userRole", "companyName", "action", "module");
    private static final Duration VENTANA_CONSULTA_AUDITADA = Duration.ofMinutes(10);
    private static final String ACCION_CONSULTA = "CONSULTA_AUDITORIA";

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

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

        Sort sortOrder = resolverOrden(sort);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortOrder);

        registrarConsultaSiCorresponde(initialLoad);

        Page<AuditLog> logs = auditLogService.getLogs(companyId, userEmail, action, module, startDate, endDate, pageable);

        return ResponseEntity.ok(new ApiResponse<>(true, "Logs de auditoría obtenidos correctamente", logs));
    }

    private Sort resolverOrden(String sort) {
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        if (!CAMPOS_ORDENABLES.contains(campo)) {
            throw new IllegalArgumentException(
                    "No se puede ordenar por \"" + campo + "\". Campos permitidos: " + String.join(", ", new TreeSet<>(CAMPOS_ORDENABLES)));
        }
        Sort orden = Sort.by(campo);
        return partes.length > 1 && "desc".equalsIgnoreCase(partes[1].trim()) ? orden.descending() : orden.ascending();
    }

    /**
     * Auditar al auditor: la decisión es del servidor y no depende de lo que envíe el cliente.
     * Se deja constancia de cada sesión de consulta (una por ventana de tiempo) para no inundar
     * el historial al paginar o filtrar; {@code initialLoad} solo fuerza el registro.
     */
    private void registrarConsultaSiCorresponde(boolean initialLoad) {
        String correo = SecurityUtils.getCurrentUserEmail();
        boolean yaRegistrada = !initialLoad && correo != null
                && auditLogRepository.existsByUserEmailAndActionAndTimestampAfter(
                        correo, ACCION_CONSULTA, AppClock.now().minus(VENTANA_CONSULTA_AUDITADA));
        if (yaRegistrada) {
            return;
        }
        String rol = SecurityUtils.getCurrentRoleNames().stream().findFirst().orElse("ROL_ACTIVO");
        auditLogService.log(
                correo,
                rol,
                SecurityUtils.getCurrentCompanyId(),
                null,
                ACCION_CONSULTA,
                "Seguridad",
                String.format("El usuario %s consultó el historial de auditoría.", correo),
                null);
    }

    private static final int EXPORT_MAX_RECORDS = 20000;

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<java.util.List<AuditLog>>> exportLogs(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (!SecurityUtils.isSuperAdmin()) {
            companyId = SecurityUtils.getCurrentCompanyId();
            if (companyId == null) {
                return ResponseEntity.status(403).body(new ApiResponse<>(false, "Acceso denegado: No tienes un ID de empresa asignado", null));
            }
        }

        Pageable pageable = PageRequest.of(0, EXPORT_MAX_RECORDS, Sort.by("timestamp").descending());
        Page<AuditLog> logs = auditLogService.getLogs(companyId, userEmail, action, module, startDate, endDate, pageable);

        boolean filtrosAplicados = userEmail != null || action != null || module != null
                || startDate != null || endDate != null || (SecurityUtils.isSuperAdmin() && companyId != null);
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        auditLogService.log(
                "EXPORTAR_AUDITORIA",
                "Seguridad",
                "El usuario " + currentUserEmail + " exportó el historial de auditoría ("
                        + (filtrosAplicados ? "con filtros aplicados, " : "completo, ")
                        + logs.getContent().size() + " registros)."
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "Logs de auditoría exportados correctamente", logs.getContent()));
    }
}
