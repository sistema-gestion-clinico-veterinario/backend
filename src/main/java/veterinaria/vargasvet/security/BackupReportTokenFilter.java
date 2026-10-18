package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Autentica el canal máquina-a-máquina utilizado por GitHub Actions para reportar respaldos. */
@Component
@RequiredArgsConstructor
public class BackupReportTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Backup-Report-Token";
    private static final String REPORT_PATH = "/internal/backups/events";

    private final BackupReportTokenVerifier tokenVerifier;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !REPORT_PATH.equals(rutaDentroDeLaAplicacion(request));
    }

    private static String rutaDentroDeLaAplicacion(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!tokenVerifier.isValid(request.getHeader(TOKEN_HEADER))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Credencial de reporte inválida\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
