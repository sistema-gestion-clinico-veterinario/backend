package veterinaria.vargasvet.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String key, int capacity, Duration period) {
        return buckets.computeIfAbsent(key, k ->
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

        boolean allowed = true;

        if (path.contains("/auth/login")) {
            allowed = resolveBucket("login:" + ip, 5, Duration.ofMinutes(1)).tryConsume(1);
        } else if (path.contains("/auth/refresh") && user != null) {
            allowed = resolveBucket("refresh:" + user, 10, Duration.ofMinutes(1)).tryConsume(1);
        } else if (path.contains("/media/upload")) {
            String key = user != null ? "upload:" + user : "upload:" + ip;
            allowed = resolveBucket(key, 10, Duration.ofMinutes(1)).tryConsume(1);
        } else if (user != null) {
            allowed = resolveBucket("global:" + user, 200, Duration.ofMinutes(1)).tryConsume(1);
        }

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intente nuevamente en un minuto.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
