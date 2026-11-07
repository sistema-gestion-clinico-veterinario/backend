package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import veterinaria.vargasvet.service.SystemLogService;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._:-]{8,100}$");

    private final SystemLogService systemLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER));
        long startedAt = System.nanoTime();
        Throwable failure = null;
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
            try {
                systemLogService.recordHttpRequest(request, correlationId,
                        response.getStatus(), durationMs, failure);
            } catch (Exception logFailure) {
                // Una indisponibilidad de observabilidad no revierte la operación atendida.
                log.warn("No se pudo finalizar la traza técnica correlationId={}", correlationId);
            }
            MDC.remove("correlationId");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/health") || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private String resolveCorrelationId(String received) {
        if (received != null && SAFE_ID.matcher(received).matches()) return received;
        return UUID.randomUUID().toString();
    }
}
