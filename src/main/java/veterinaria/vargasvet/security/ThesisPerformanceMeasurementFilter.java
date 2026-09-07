package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import veterinaria.vargasvet.domain.enums.ThesisMeasurementPhase;
import veterinaria.vargasvet.domain.enums.ThesisPerformanceOperation;
import veterinaria.vargasvet.service.ThesisPerformanceMeasurementService;
import veterinaria.vargasvet.util.AppClock;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThesisPerformanceMeasurementFilter extends OncePerRequestFilter {

    public static final String SESSION_HEADER = "X-Thesis-Measurement-Session";
    public static final String PHASE_HEADER = "X-Thesis-Measurement-Phase";

    private final ThesisPerformanceMeasurementService measurementService;

    @Value("${thesis.performance-measurement.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UUID sessionId = enabled ? parseSessionId(request.getHeader(SESSION_HEADER)) : null;
        ThesisMeasurementPhase phase = sessionId != null ? parsePhase(request.getHeader(PHASE_HEADER)) : null;
        if (sessionId == null || phase == null) {
            filterChain.doFilter(request, response);
            return;
        }

        LocalDateTime requestedAt = AppClock.now();
        long startedNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Object patternAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String routePattern = patternAttribute instanceof String value ? value : null;
            ThesisPerformanceOperation.resolve(request.getMethod(), routePattern).ifPresent(operation -> {
                Integer userId = SecurityUtils.getCurrentUserId();
                if (userId == null) {
                    return;
                }

                BigDecimal durationMs = BigDecimal.valueOf(System.nanoTime() - startedNanos)
                        .divide(BigDecimal.valueOf(1_000_000), 3, RoundingMode.HALF_UP);
                try {
                    measurementService.record(
                            sessionId,
                            phase,
                            requestedAt,
                            operation,
                            routePattern,
                            request.getMethod(),
                            durationMs,
                            response.getStatus(),
                            userId,
                            SecurityUtils.getCurrentCompanyId());
                } catch (RuntimeException ex) {
                    log.warn("No se pudo guardar la medición de rendimiento de la sesión {}", sessionId, ex);
                }
            });
        }
    }

    private UUID parseSessionId(String rawValue) {
        if (rawValue == null || rawValue.length() > 36) {
            return null;
        }
        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ThesisMeasurementPhase parsePhase(String rawValue) {
        if (rawValue == null || rawValue.length() > 16) {
            return null;
        }
        try {
            return ThesisMeasurementPhase.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
