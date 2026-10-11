package veterinaria.vargasvet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Resumen de auditoría para el dashboard.
 * El correo, la IP, el dispositivo y el detalle libre solo se consultan en el
 * módulo de auditoría, que cuenta con autorización propia.
 */
@Data
@Builder
public class DashboardAuditSummary {
    private Long id;
    private LocalDateTime timestamp;
    private String userRole;
    private String companyName;
    private String action;
    private String module;
}
