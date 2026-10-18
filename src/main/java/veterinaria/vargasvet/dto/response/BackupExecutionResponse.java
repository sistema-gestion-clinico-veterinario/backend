package veterinaria.vargasvet.dto.response;

import veterinaria.vargasvet.domain.enums.BackupComponentStatus;
import veterinaria.vargasvet.domain.enums.BackupRunStatus;
import veterinaria.vargasvet.domain.enums.BackupTriggerType;

import java.time.Instant;
import java.util.UUID;

public record BackupExecutionResponse(
        Long id,
        String runId,
        int runAttempt,
        UUID recoveryPointId,
        String retentionPolicy,
        BackupTriggerType triggerType,
        BackupRunStatus status,
        BackupComponentStatus databaseStatus,
        BackupComponentStatus restrictionsStatus,
        BackupComponentStatus storageStatus,
        boolean restorationVerified,
        Instant startedAt,
        Instant finishedAt,
        String commitSha,
        String workflowUrl
) {
}
