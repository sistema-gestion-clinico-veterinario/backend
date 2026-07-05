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
import veterinaria.vargasvet.repository.RoleRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.UsuarioPrincipal;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWTFilter extends GenericFilterBean {
    private final TokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(httpRequest);

        if (token != null) {
            try {
                Authentication authentication = tokenProvider.getAuthentication(token);

                boolean esSuperAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

                if (!esSuperAdmin) {
                    String email = authentication.getName();
                    boolean bloqueado = usuarioRepository.findByEmailWithCompany(email).map(usuario -> {
                        if (!usuario.isActivo()) return true;
                        return usuario.getCompany() != null && !usuario.getCompany().isActivo();
                    }).orElse(true);

                    if (bloqueado) {
                        SecurityContextHolder.clearContext();
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        httpResponse.setContentType("application/json");
                        httpResponse.getWriter().write("{\"error\":\"Acceso denegado. La empresa o el usuario está inactivo.\"}");
                        return;
                    }

                    String activeRole = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .filter(a -> a.startsWith("ROLE_"))
                            .filter(a -> !a.equals("ROLE_SUPER_ADMIN"))
                            .findFirst()
                            .orElse(null);
                    if (activeRole != null) {
                        Integer companyId = authentication.getPrincipal() instanceof UsuarioPrincipal up
                                ? up.getCompanyId() : null;
                        boolean roleInactivo;
                        if (companyId != null) {
                            roleInactivo = roleRepository.findByNameAndCompanyId(activeRole, companyId)
                                    .map(role -> !role.isActivo())
                                    .orElse(false);
                        } else {
                            roleInactivo = roleRepository.findAllByName(activeRole).stream()
                                    .filter(r -> r.getCompany() == null)
                                    .findFirst()
                                    .map(role -> !role.isActivo())
                                    .orElse(false);
                        }
                        if (roleInactivo) {
                            SecurityContextHolder.clearContext();
                            HttpServletResponse httpResponse = (HttpServletResponse) response;
                            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            httpResponse.setContentType("application/json");
                            httpResponse.getWriter().write("{\"error\":\"Acceso denegado. El rol asignado se encuentra desactivado.\"}");
                            return;
                        }
                    }
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
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
