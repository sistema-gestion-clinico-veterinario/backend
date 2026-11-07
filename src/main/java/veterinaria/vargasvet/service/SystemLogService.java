package veterinaria.vargasvet.service;

import jakarta.servlet.http.HttpServletRequest;

public interface SystemLogService {
    void recordHttpRequest(HttpServletRequest request, String correlationId,
                           int statusCode, long durationMs, Throwable failure);
}
