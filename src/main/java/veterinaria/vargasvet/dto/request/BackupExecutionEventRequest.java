package veterinaria.vargasvet.dto.request;

import jakarta.validation.constraints.*;
import veterinaria.vargasvet.domain.enums.*;

import java.time.Instant;
import java.util.UUID;

public record BackupExecutionEventRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{1,20}$") String runId,
        @Min(1) int runAttempt,
        UUID recoveryPointId,
        @Pattern(regexp = "^p0[1-3]$") String retentionPolicy,
        @NotNull BackupTriggerType triggerType,
        @NotNull BackupEventPhase phase,
        @NotNull BackupRunStatus status,
        @NotNull BackupComponentStatus databaseStatus,
        @NotNull BackupComponentStatus restrictionsStatus,
        @NotNull BackupComponentStatus storageStatus,
        @NotNull Instant startedAt,
        Instant finishedAt,
        @Pattern(regexp = "^[0-9a-f]{40}$") String commitSha,
        @NotBlank @Size(max = 500) @Pattern(regexp = "^https://github\\.com/.*$") String workflowUrl
) {
}
