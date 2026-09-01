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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.domain.enums.RolePurpose;

import java.io.IOException;

@Component
@RequiredArgsConstructor
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
                UsuarioPrincipal principal = authentication.getPrincipal() instanceof UsuarioPrincipal value
                        ? value : null;
                if (principal == null || principal.getActiveRoleId() == null) {
                    throw new org.springframework.security.authentication.BadCredentialsException(
                            "La sesión no contiene un rol activo válido");
                }

                var activeAssignment = usuarioPorRolRepository
                        .findActiveAssignmentByUsuarioIdAndRoleId(principal.getId(), principal.getActiveRoleId())
                        .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                                "El rol de la sesión ya no está disponible"));
                if (activeAssignment.getRol().getPermissionVersion() != principal.getPermissionVersion()) {
                    throw new org.springframework.security.authentication.CredentialsExpiredException(
                            "Los permisos de la sesión cambiaron; actualice la sesión");
                }
                boolean esSuperAdmin = activeAssignment.getRol().getPurpose() == RolePurpose.PLATFORM_ADMIN;

                var currentUser = usuarioRepository.findByEmailWithCompany(email)
                        .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                                "La cuenta de la sesión ya no existe"));
                if (currentUser.getCredentialsVersion() != principal.getCredentialsVersion()) {
                    throw new org.springframework.security.authentication.CredentialsExpiredException(
                            "La sesión fue invalidada por un evento de seguridad");
                }
                boolean bloqueado = !currentUser.isActivo()
                        || (!esSuperAdmin && currentUser.getCompany() != null
                        && !currentUser.getCompany().isActivo());

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
                || path.equals("/auth/email-change/confirm-current")
                || path.equals("/auth/email-change/confirm-new")
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
