package veterinaria.vargasvet.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Profile("!e2e")
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.login-per-minute:5}")
    private int loginPerMinute;

    @Value("${app.rate-limit.refresh-per-minute:10}")
    private int refreshPerMinute;

    @Value("${app.rate-limit.password-reset-per-15-minutes:5}")
    private int passwordResetPerWindow;

    @Value("${app.rate-limit.realtime-ticket-per-minute:10}")
    private int realtimeTicketPerMinute;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    private Bucket resolveBucket(String key, int capacity, Duration period) {
        return buckets.get(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, period)))
                        .build()
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String user = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : null;

        boolean allowed;

        if (path.contains("/auth/login")) {
            allowed = resolveBucket("login:" + ip, loginPerMinute, Duration.ofMinutes(1)).tryConsume(1);
        } else if (path.contains("/auth/refresh")) {
            // El access token ya está vencido en este endpoint (por eso se refresca),
            // por lo que JWTFilter nunca autentica esta request: se limita por IP, no por usuario.
            allowed = resolveBucket("refresh:" + ip, refreshPerMinute, Duration.ofMinutes(1)).tryConsume(1);
        } else if (path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")
                || path.contains("/auth/resend-verification")) {
            allowed = resolveBucket("account-recovery:" + ip, passwordResetPerWindow,
                    Duration.ofMinutes(15)).tryConsume(1);
        } else if (path.contains("/realtime/ticket")) {
            String key = user != null ? "realtime-ticket:" + user : "realtime-ticket:" + ip;
            allowed = resolveBucket(key, realtimeTicketPerMinute, Duration.ofMinutes(1)).tryConsume(1);
        } else if (path.contains("/media/upload")) {
            String key = user != null ? "upload:" + user : "upload:" + ip;
            allowed = resolveBucket(key, 10, Duration.ofMinutes(1)).tryConsume(1);
        } else if (user != null) {
            allowed = resolveBucket("global:" + user, 200, Duration.ofMinutes(1)).tryConsume(1);
        } else {
            // Endpoints públicos sin autenticación (registro, recuperación de contraseña, etc.):
            // sin esto quedaban sin ningún límite y expuestos a abuso.
            allowed = resolveBucket("anon:" + ip, 60, Duration.ofMinutes(1)).tryConsume(1);
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intente nuevamente en un minuto.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
