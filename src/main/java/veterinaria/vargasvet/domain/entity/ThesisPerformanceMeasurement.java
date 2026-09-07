package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import veterinaria.vargasvet.domain.enums.ThesisMeasurementPhase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "thesis_performance_measurements", indexes = {
        @Index(name = "idx_thesis_perf_session_user", columnList = "measurement_session_id,user_id"),
        @Index(name = "idx_thesis_perf_requested_at", columnList = "requested_at")
})
public class ThesisPerformanceMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "measurement_session_id", nullable = false)
    private UUID measurementSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 16)
    private ThesisMeasurementPhase phase;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "operation_code", nullable = false, length = 64)
    private String operationCode;

    @Column(name = "operation_name", nullable = false, length = 120)
    private String operationName;

    @Column(name = "route_template", nullable = false, length = 255)
    private String routeTemplate;

    @Column(name = "http_method", nullable = false, length = 8)
    private String httpMethod;

    @Column(name = "duration_ms", nullable = false, precision = 15, scale = 3)
    private BigDecimal durationMs;

    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;

    @Column(name = "successful", nullable = false)
    private Boolean successful;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "company_id")
    private Integer companyId;
}
