package veterinaria.vargasvet.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.SystemLog;
import veterinaria.vargasvet.repository.SystemLogRepository;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.SystemLogService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.DispositivoInfo;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemLogServiceImpl implements SystemLogService {
    private final SystemLogRepository repository;
    private final ClientIpResolver clientIpResolver;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHttpRequest(HttpServletRequest request, String correlationId,
                                  int statusCode, long durationMs, Throwable failure) {
        try {
            repository.save(SystemLog.builder()
                    .timestamp(AppClock.now())
                    .correlationId(correlationId)
                    .category("HTTP")
                    .event("HTTP_REQUEST")
                    .outcome(resolveOutcome(statusCode))
                    .httpMethod(limit(request.getMethod(), 10))
                    .requestPath(limit(request.getRequestURI(), 500))
                    .statusCode(statusCode)
                    .durationMs(Math.max(0, durationMs))
                    .userEmail(limit(SecurityUtils.getCurrentUserEmail(), 255))
                    .companyId(SecurityUtils.getCurrentCompanyId())
                    .ipAddress(limit(clientIpResolver.resolve(request), 64))
                    .dispositivo(limit(DispositivoInfo.describir(request.getHeader("User-Agent")), 120))
                    .errorType(failure == null ? null : limit(failure.getClass().getSimpleName(), 150))
                    .build());
        } catch (Exception ex) {
            // La observabilidad nunca debe interrumpir una operación de negocio.
            log.warn("No se pudo persistir el registro técnico correlationId={}", correlationId);
        }
    }

    private String resolveOutcome(int statusCode) {
        if (statusCode == 401 || statusCode == 403) return "DENIED";
        if (statusCode >= 400) return "FAILED";
        if (statusCode >= 100) return "SUCCESS";
        return "UNKNOWN";
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
