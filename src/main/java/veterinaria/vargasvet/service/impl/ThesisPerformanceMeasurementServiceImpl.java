package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.ThesisPerformanceMeasurement;
import veterinaria.vargasvet.domain.enums.ThesisMeasurementPhase;
import veterinaria.vargasvet.domain.enums.ThesisPerformanceOperation;
import veterinaria.vargasvet.dto.response.ThesisPerformanceMeasurementResponse;
import veterinaria.vargasvet.repository.ThesisPerformanceMeasurementRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ThesisPerformanceMeasurementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThesisPerformanceMeasurementServiceImpl implements ThesisPerformanceMeasurementService {

    private final ThesisPerformanceMeasurementRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID measurementSessionId,
            ThesisMeasurementPhase phase,
            LocalDateTime requestedAt,
            ThesisPerformanceOperation operation,
            String routeTemplate,
            String httpMethod,
            BigDecimal durationMs,
            int httpStatus,
            Integer userId,
            Integer companyId) {
        repository.save(ThesisPerformanceMeasurement.builder()
                .measurementSessionId(measurementSessionId)
                .phase(phase)
                .requestedAt(requestedAt)
                .operationCode(operation.getCode())
                .operationName(operation.getDisplayName())
                .routeTemplate(routeTemplate)
                .httpMethod(httpMethod)
                .durationMs(durationMs)
                .httpStatus(httpStatus)
                .successful(httpStatus >= 200 && httpStatus < 400)
                .userId(userId)
                .companyId(companyId)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThesisPerformanceMeasurementResponse> findOwnSession(UUID measurementSessionId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("Se requiere un usuario autenticado para consultar la sesión de medición");
        }
        return repository.findAllByMeasurementSessionIdAndUserIdOrderByRequestedAtAscIdAsc(measurementSessionId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ThesisPerformanceMeasurementResponse toResponse(ThesisPerformanceMeasurement entity) {
        return ThesisPerformanceMeasurementResponse.builder()
                .id(entity.getId())
                .measurementSessionId(entity.getMeasurementSessionId())
                .phase(entity.getPhase().name())
                .requestedAt(entity.getRequestedAt())
                .operationCode(entity.getOperationCode())
                .operationName(entity.getOperationName())
                .routeTemplate(entity.getRouteTemplate())
                .httpMethod(entity.getHttpMethod())
                .durationMs(entity.getDurationMs())
                .httpStatus(entity.getHttpStatus())
                .successful(entity.getSuccessful())
                .companyId(entity.getCompanyId())
                .build();
    }
}
