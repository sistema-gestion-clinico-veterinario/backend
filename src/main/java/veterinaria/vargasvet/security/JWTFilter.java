package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JWTFilter extends GenericFilterBean {
    private final TokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPorRolRepository usuarioPorRolRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicAuthEndpoint(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(httpRequest);

        if (token != null) {
            try {
                Authentication authentication = tokenProvider.getAuthentication(token);

                String email = authentication.getName();
                String activeRole = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .findFirst()
                        .orElse(null);
                if (activeRole == null || !usuarioPorRolRepository.hasActiveAssignedRole(email, activeRole)) {
                    throw new org.springframework.security.authentication.BadCredentialsException(
                            "El rol de la sesión ya no está disponible");
                }
                boolean esSuperAdmin = "ROLE_SUPER_ADMIN".equals(activeRole);

                boolean bloqueado = usuarioRepository.findByEmailWithCompany(email).map(usuario -> {
                    if (!usuario.isActivo()) return true;
                    return !esSuperAdmin && usuario.getCompany() != null && !usuario.getCompany().isActivo();
                }).orElse(true);

                if (bloqueado) {
                    SecurityContextHolder.clearContext();
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"Acceso denegado. La empresa o el usuario está inactivo.\"}");
                    return;
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        return path.equals("/auth/login")
                || path.equals("/auth/refresh")
                || path.equals("/auth/logout")
                || path.equals("/auth/forgot-password")
                || path.equals("/auth/reset-password")
                || path.equals("/auth/validate-reset-token")
                || path.equals("/auth/setup-account")
                || path.equals("/auth/resend-verification")
                || path.startsWith("/auth/register")
                || path.startsWith("/setup/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/ws/")
                || path.equals("/error")
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/media/"));
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. Try access_token cookie first
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        // 2. Fallback to Authorization: Bearer header
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
