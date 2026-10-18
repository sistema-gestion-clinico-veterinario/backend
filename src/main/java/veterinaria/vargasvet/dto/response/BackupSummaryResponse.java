package veterinaria.vargasvet.dto.response;

import java.time.Instant;

public record BackupSummaryResponse(
        String health,
        boolean reportingConfigured,
        Instant lastExecutionAt,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        long successCount30Days,
        long problemCount30Days,
        Instant nextScheduledAt,
        int maxHoursWithoutSuccess
) {
}
