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
import java.util.Set;

@Component
@Profile("!e2e")
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> AUTHENTICATION_POST_PATHS = Set.of(
            "/auth/login",
            "/auth/admin-login",
            "/auth/google/intent",
            "/auth/google/exchange",
            "/auth/google/reactivate"
    );

    private static final Set<String> RECOVERY_POST_PATHS = Set.of(
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/validate-reset-token",
            "/auth/resend-verification",
            "/auth/resend-verification-by-token",
            "/auth/account/reactivate",
            "/auth/setup-account"
    );

    private final SharedRateLimitService sharedRateLimitService;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(SharedRateLimitService sharedRateLimitService, ClientIpResolver clientIpResolver) {
        this.sharedRateLimitService = sharedRateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    @Value("${app.rate-limit.login-per-minute:20}")
    private int loginPerMinute;

    @Value("${app.rate-limit.refresh-per-minute:10}")
    private int refreshPerMinute;

    @Value("${app.rate-limit.password-reset-per-15-minutes:5}")
    private int passwordResetPerWindow;

    @Value("${app.rate-limit.realtime-ticket-per-minute:10}")
    private int realtimeTicketPerMinute;

    @Value("${app.rate-limit.switch-role-per-minute:10}")
    private int switchRolePerMinute;

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

        String ip = clientIpResolver.resolve(request);
        String user = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : null;

        boolean allowed;
        Duration retryAfter = Duration.ofMinutes(1);

        if ("POST".equalsIgnoreCase(method) && matchesAnySuffix(path, AUTHENTICATION_POST_PATHS)) {
            allowed = sharedRateLimitService.tryConsume(
                    "login-ip", ip, loginPerMinute, Duration.ofMinutes(1));
        } else if (path.contains("/auth/refresh")) {
            // El access token ya está vencido en este endpoint (por eso se refresca),
            // por lo que JWTFilter nunca autentica esta request: se limita por IP, no por usuario.
            allowed = sharedRateLimitService.tryConsume(
                    "refresh-ip", ip, refreshPerMinute, Duration.ofMinutes(1));
        } else if (("POST".equalsIgnoreCase(method) && matchesAnySuffix(path, RECOVERY_POST_PATHS))
                || ("GET".equalsIgnoreCase(method) && path.endsWith("/auth/validate-reset-token"))
                || path.contains("/auth/email-change/")) {
            retryAfter = Duration.ofMinutes(15);
            allowed = sharedRateLimitService.tryConsume(
                    "account-recovery-ip", ip, passwordResetPerWindow, retryAfter);
        } else if (path.contains("/auth/switch-role")) {
            String key = user != null ? "switch-role:" + user : "switch-role:" + ip;
            allowed = resolveBucket(key, switchRolePerMinute, Duration.ofMinutes(1)).tryConsume(1);
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
            writeRateLimitResponse(response, retryAfter);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean matchesAnySuffix(String requestPath, Set<String> endpoints) {
        return endpoints.stream().anyMatch(requestPath::endsWith);
    }

    private void writeRateLimitResponse(HttpServletResponse response, Duration retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter.toSeconds())));
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Demasiadas solicitudes. Intente nuevamente más tarde.\",\"data\":null}");
    }
}
