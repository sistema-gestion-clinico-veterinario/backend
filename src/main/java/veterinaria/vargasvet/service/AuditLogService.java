package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.AuditLog;
import java.time.LocalDateTime;

public interface AuditLogService {

    /**
     * Registra un log de auditoría capturando automáticamente el usuario y empresa del contexto de seguridad.
     */
    void log(String action, String module, String details);

    /**
     * Registra un log de auditoría con datos explícitos (útil para login/logout o procesos externos).
     */
    void log(String email, String role, Integer companyId, String companyName, String action, String module, String details, String ipAddress);

    /**
     * Obtiene los logs filtrados de forma paginada.
     */
    Page<AuditLog> getLogs(Integer companyId, String userEmail, String action, String module, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
