package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.dto.AuditLogDTO;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishAfterCommit(AuditLog saved) {
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

                messagingTemplate.convertAndSend("/topic/audit-logs", dto);

                if (saved.getCompanyId() != null) {
                    messagingTemplate.convertAndSend("/topic/audit-logs/" + saved.getCompanyId(), dto);
                }
            } catch (Exception e) {
                log.warn("No se pudo publicar el evento de auditoría por WebSocket (auditId={})", saved.getId());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
