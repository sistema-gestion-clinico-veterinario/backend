package veterinaria.vargasvet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces a single upper bound for the page/size convention used by the API.
 * This prevents callers from bypassing service-level pagination with very large
 * values while the existing clients are migrated to smaller, searchable pages.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class PaginationParameterFilter extends OncePerRequestFilter {

    private final int maxPageSize;

    public PaginationParameterFilter(@Value("${api.pagination.max-size:1000}") int maxPageSize) {
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("api.pagination.max-size debe ser mayor a cero");
        }
        this.maxPageSize = maxPageSize;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Integer page = parseParameter(request, response, "page");
        if (response.isCommitted()) return;

        Integer size = parseParameter(request, response, "size");
        if (response.isCommitted()) return;

        if (page != null && page < 0) {
            reject(response, "El parámetro page no puede ser negativo");
            return;
        }
        if (size != null && (size < 1 || size > maxPageSize)) {
            reject(response, "El parámetro size debe estar entre 1 y " + maxPageSize);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Integer parseParameter(HttpServletRequest request,
                                   HttpServletResponse response,
                                   String name) throws IOException {
        String rawValue = request.getParameter(name);
        if (rawValue == null || rawValue.isBlank()) return null;
        try {
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException ex) {
            reject(response, "El parámetro " + name + " debe ser un número entero");
            return null;
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
        response.flushBuffer();
    }
}
