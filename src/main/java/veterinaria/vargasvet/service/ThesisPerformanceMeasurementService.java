package veterinaria.vargasvet.service;

import veterinaria.vargasvet.domain.enums.ThesisMeasurementPhase;
import veterinaria.vargasvet.domain.enums.ThesisPerformanceOperation;
import veterinaria.vargasvet.dto.response.ThesisPerformanceMeasurementResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ThesisPerformanceMeasurementService {
    void record(
            UUID measurementSessionId,
            ThesisMeasurementPhase phase,
            LocalDateTime requestedAt,
            ThesisPerformanceOperation operation,
            String routeTemplate,
            String httpMethod,
            BigDecimal durationMs,
            int httpStatus,
            Integer userId,
            Integer companyId);

    List<ThesisPerformanceMeasurementResponse> findOwnSession(UUID measurementSessionId);
}
