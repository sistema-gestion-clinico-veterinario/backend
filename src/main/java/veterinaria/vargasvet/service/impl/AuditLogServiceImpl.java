package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.dto.AuditLogDTO;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            // Eliminar la columna con tipo incorrecto (bytea) y recrearla como VARCHAR(255) al inicio
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP COLUMN IF EXISTS user_email");
            jdbcTemplate.execute("ALTER TABLE audit_logs ADD COLUMN user_email VARCHAR(255)");
            System.out.println("SCHEMA FIX: Column audit_logs.user_email successfully recreated as VARCHAR(255).");
        } catch (Exception e) {
            System.err.println("SCHEMA FIX WARNING: Could not recreate user_email column: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void log(String action, String module, String details) {
        String email = SecurityUtils.getCurrentUserEmail();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        
        // Extraer rol activo de las authorities del SecurityContext
        String role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .orElse(null);
        }

        String ip = getClientIp();

        log(email, role, companyId, null, action, module, details, ip);
    }

    @Override
    @Transactional
    public void log(String email, String role, Integer companyId, String companyName, String action, String module, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userEmail(email)
                .userRole(role)
                .companyId(companyId)
                .companyName(companyName)
                .action(action)
                .module(module)
                .details(details)
                .ipAddress(ipAddress != null ? ipAddress : getClientIp())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);

        // Transmitir en tiempo real a través de WebSockets de forma segura
        try {
            AuditLogDTO dto = AuditLogDTO.builder()
                    .id(saved.getId())
                    .timestamp(saved.getTimestamp() != null ? saved.getTimestamp().toString() : null)
                    .userEmail(saved.getUserEmail())
                    .userRole(saved.getUserRole())
                    .companyId(saved.getCompanyId())
                    .companyName(saved.getCompanyName())
                    .action(saved.getAction())
                    .module(saved.getModule())
                    .details(saved.getDetails())
                    .ipAddress(saved.getIpAddress())
                    .build();

            // Canal global para Super Administradores
            messagingTemplate.convertAndSend("/topic/audit-logs", dto);
            
            // Canal específico para Administradores de la clínica actual
            if (companyId != null) {
                messagingTemplate.convertAndSend("/topic/audit-logs/" + companyId, dto);
            }
        } catch (Exception e) {
            System.err.println("WS ERROR: No se pudo transmitir el log de auditoría por WebSockets: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Integer companyId, String userEmail, String action, String module, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.filterLogs(companyId, userEmail, action, module, startDate, endDate, pageable);
    }

    private String getClientIp() {
        if (httpServletRequest == null) return null;
        try {
            String ip = httpServletRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpServletRequest.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
