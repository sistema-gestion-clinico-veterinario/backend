package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CookieSecurityFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final Set<String> allowedOrigins;

    public CookieSecurityFilter(@Value("${cors.allowed-origins:https://systemvetfrontend.vercel.app}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .map(this::normalizeOrigin)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getServletPath().startsWith("/auth/")) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setDateHeader(HttpHeaders.EXPIRES, 0);
        }

        boolean authenticationEndpoint = request.getServletPath().startsWith("/auth/");
        if (!SAFE_METHODS.contains(request.getMethod())
                && (hasAuthenticationCookie(request) || authenticationEndpoint)
                && !hasTrustedOrigin(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Origen de solicitud no permitido\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean hasAuthenticationCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        return Arrays.stream(cookies).anyMatch(cookie ->
                "access_token".equals(cookie.getName()) || "refresh_token".equals(cookie.getName()));
    }

    private boolean hasTrustedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            return allowedOrigins.contains(normalizeOrigin(origin));
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) return false;
        try {
            URI uri = URI.create(referer);
            int port = uri.getPort();
            String refererOrigin = uri.getScheme() + "://" + uri.getHost()
                    + (port > 0 ? ":" + port : "");
            return allowedOrigins.contains(normalizeOrigin(refererOrigin));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String normalizeOrigin(String origin) {
        String normalized = origin == null ? "" : origin.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(java.util.Locale.ROOT);
    }
}
