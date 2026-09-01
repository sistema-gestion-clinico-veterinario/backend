package veterinaria.vargasvet.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.AuditLog;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.util.AppClock;

import java.util.Locale;

/**
 * Auditoria de eventos de autenticacion que deben persistir incluso cuando la
 * operacion principal termina con credenciales invalidas y hace rollback.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(Usuario knownUser, String attemptedIdentifier, String reason) {
        String subjectHash = SecurityTokenUtils.hash(normalize(attemptedIdentifier)).substring(0, 12);
        save(knownUser, "LOGIN_FALLIDO",
                "Intento de inicio de sesión rechazado (" + reason + ", referencia " + subjectHash + ").");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Usuario usuario, String action, String details) {
        save(usuario, action, details);
    }

    private void save(Usuario usuario, String action, String details) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        auditLogRepository.save(AuditLog.builder()
                .timestamp(AppClock.now())
                .userEmail(usuario != null ? usuario.getEmail() : null)
                .userRole(null)
                .companyId(usuario != null && usuario.getCompany() != null ? usuario.getCompany().getId() : null)
                .companyName(usuario != null && usuario.getCompany() != null ? usuario.getCompany().getName() : null)
                .action(action)
                .module("Seguridad")
                .details(details)
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .build());
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }
}
