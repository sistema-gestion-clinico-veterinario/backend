package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ThesisPerformanceMeasurementResponse {
    private Long id;
    private UUID measurementSessionId;
    private String phase;
    private LocalDateTime requestedAt;
    private String operationCode;
    private String operationName;
    private String routeTemplate;
    private String httpMethod;
    private BigDecimal durationMs;
    private Integer httpStatus;
    private Boolean successful;
    private Integer companyId;
}
