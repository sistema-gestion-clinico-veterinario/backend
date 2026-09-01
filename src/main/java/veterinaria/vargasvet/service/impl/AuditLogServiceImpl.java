package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CompanyRepository companyRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @Override
    @Transactional
    public void log(String action, String module, String details) {
        log(null, action, module, details);
    }

    @Override
    @Transactional
    public void log(Integer companyId, String action, String module, String details) {
        String email = SecurityUtils.getCurrentUserEmail();
        
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

        // Resolver nombre de empresa
        String companyName = null;
        Integer finalCompanyId = companyId;
        if (finalCompanyId == null) {
            finalCompanyId = SecurityUtils.getCurrentCompanyId();
        }

        if (finalCompanyId != null) {
            try {
                companyName = companyRepository.findById(finalCompanyId)
                        .map(Company::getName)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("No se pudo resolver el nombre de empresa para auditoría (companyId={})", finalCompanyId);
            }
        }

        // Determinar el origen (desde dónde se originó la llamada en el frontend)
        String finalDetails = details;
        if (httpServletRequest != null) {
            String referer = httpServletRequest.getHeader("Referer");
            if (referer == null) {
                referer = httpServletRequest.getHeader("referer");
            }
            if (referer != null) {
                String refLower = referer.toLowerCase();
                String detailsSuffix = "";
                
                if (refLower.contains("/dashboard")) {
                    detailsSuffix = " desde el panel (Dashboard).";
                } else if (refLower.contains("/mascotas")) {
                    detailsSuffix = " desde la sección de Mascotas.";
                } else if (refLower.contains("/citas/agenda") || refLower.contains("/citas")) {
                    detailsSuffix = " desde la agenda de Citas.";
                } else if (refLower.contains("/admin/clientes")) {
                    detailsSuffix = " desde el mantenedor de Clientes.";
                } else if (refLower.contains("/admin/empleados")) {
                    detailsSuffix = " desde el mantenedor de Empleados.";
                } else if (refLower.contains("/admin/auditoria")) {
                    detailsSuffix = " desde la sección de Auditoría.";
                }
                
                if (!detailsSuffix.isEmpty() && finalDetails != null) {
                    if (finalDetails.endsWith(".")) {
                        finalDetails = finalDetails.substring(0, finalDetails.length() - 1);
                    }
                    finalDetails = finalDetails + detailsSuffix;
                }
            }
        }

        log(email, role, finalCompanyId, companyName, action, module, finalDetails, ip);
    }

    @Override
    @Transactional
    public void log(String email, String role, Integer companyId, String companyName, String action, String module, String details, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(veterinaria.vargasvet.util.AppClock.now())
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

        Runnable publish = () -> {
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
                log.warn("No se pudo publicar el evento de auditoría por WebSocket (auditId={})", saved.getId());
            }
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() { publish.run(); }
                    });
        } else {
            publish.run();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(Integer companyId, String userEmail, String action, String module, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Integer resolvedCompanyId = SecurityUtils.isSuperAdmin()
                ? companyId
                : SecurityUtils.getCurrentCompanyId();
        if (!SecurityUtils.isSuperAdmin() && resolvedCompanyId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "El usuario autenticado no tiene una empresa asignada");
        }
        return auditLogRepository.filterLogs(resolvedCompanyId, userEmail, action, module,
                startDate, endDate, pageable);
    }

    private String getClientIp() {
        if (httpServletRequest == null) return null;
        try {
            // ForwardedHeaderFilter ya normaliza remoteAddr cuando la aplicacion
            // corre tras el proxy. No confiar directamente en un X-Forwarded-For
            // enviado por el cliente evita registrar una IP facilmente falsificable.
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
