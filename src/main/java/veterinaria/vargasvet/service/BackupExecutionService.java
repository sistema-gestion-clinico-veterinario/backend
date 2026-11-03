package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.BackupExecution;
import veterinaria.vargasvet.domain.enums.BackupEventPhase;
import veterinaria.vargasvet.domain.enums.BackupComponentStatus;
import veterinaria.vargasvet.domain.enums.BackupRunStatus;
import veterinaria.vargasvet.dto.request.BackupExecutionEventRequest;
import veterinaria.vargasvet.dto.response.BackupExecutionResponse;
import veterinaria.vargasvet.repository.BackupExecutionRepository;
import veterinaria.vargasvet.util.AppClock;

@Service
@RequiredArgsConstructor
public class BackupExecutionService {

    private final BackupExecutionRepository repository;

    @Transactional
    public void record(BackupExecutionEventRequest request) {
        BackupExecution execution = repository.findByRunIdAndRunAttempt(request.runId(), request.runAttempt())
                .orElseGet(BackupExecution::new);

        // Una ejecución finalizada es inmutable. Los reintentos de GitHub usan
        // runAttempt distinto y los reenvíos de red no pueden alterar el resultado.
        if (execution.getId() != null && execution.getStatus() != BackupRunStatus.IN_PROGRESS) {
            return;
        }
        if (request.phase() == BackupEventPhase.STARTED
                && (request.status() != BackupRunStatus.IN_PROGRESS || request.finishedAt() != null)) {
            throw new IllegalArgumentException("Un inicio debe permanecer en progreso y no tener finishedAt");
        }
        if (request.phase() == BackupEventPhase.COMPLETED
                && (request.status() == BackupRunStatus.IN_PROGRESS || request.finishedAt() == null)) {
            throw new IllegalArgumentException("Una ejecución finalizada requiere finishedAt");
        }
        if (request.finishedAt() != null && request.finishedAt().isBefore(request.startedAt())) {
            throw new IllegalArgumentException("finishedAt no puede ser anterior a startedAt");
        }

        execution.setRunId(request.runId());
        execution.setRunAttempt(request.runAttempt());
        execution.setRecoveryPointId(request.recoveryPointId());
        execution.setRetentionPolicy(request.retentionPolicy());
        execution.setTriggerType(request.triggerType());
        execution.setStatus(request.status());
        execution.setDatabaseStatus(request.databaseStatus());
        execution.setRestrictionsStatus(request.restrictionsStatus());
        execution.setStorageStatus(request.storageStatus());
        execution.setStartedAt(request.startedAt());
        execution.setFinishedAt(request.finishedAt());
        execution.setCommitSha(request.commitSha());
        execution.setWorkflowUrl(request.workflowUrl());
        execution.setProtectedSizeBytes(request.protectedSizeBytes());
        execution.setProtectedFileCount(request.protectedFileCount());
        execution.setCompressionAlgorithm(request.compressionAlgorithm());
        execution.setEncryptionAlgorithm(request.encryptionAlgorithm());
        execution.setDatabaseRestoreVerified(Boolean.TRUE.equals(request.databaseRestoreVerified()));
        execution.setDatabaseRestoreDurationSeconds(request.databaseRestoreDurationSeconds());
        BackupComponentStatus recoveryStatus = request.recoveryStatus() == null
                ? (request.phase() == BackupEventPhase.STARTED
                    ? BackupComponentStatus.PENDING : BackupComponentStatus.SKIPPED)
                : request.recoveryStatus();
        boolean integralVerified = Boolean.TRUE.equals(request.integralRecoveryVerified());
        if (integralVerified && recoveryStatus != BackupComponentStatus.SUCCESS) {
            throw new IllegalArgumentException("Una recuperación integral verificada requiere estado SUCCESS");
        }
        if (integralVerified && request.integralRecoveryDurationSeconds() == null) {
            throw new IllegalArgumentException("Una recuperación integral verificada requiere duración");
        }
        execution.setRecoveryStatus(recoveryStatus);
        execution.setIntegralRecoveryVerified(integralVerified);
        execution.setIntegralRecoveryDurationSeconds(request.integralRecoveryDurationSeconds());
        execution.setIntegralRecoveryTestedAt(
                recoveryStatus == BackupComponentStatus.SUCCESS || recoveryStatus == BackupComponentStatus.FAILED
                        ? request.finishedAt() : null);
        execution.setUpdatedAt(AppClock.instantNow());
        repository.save(execution);
    }

    @Transactional(readOnly = true)
    public Page<BackupExecutionResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return repository.findAllByOrderByStartedAtDesc(PageRequest.of(safePage, safeSize)).map(this::toResponse);
    }

    private BackupExecutionResponse toResponse(BackupExecution execution) {
        return new BackupExecutionResponse(
                execution.getId(), execution.getRunId(), execution.getRunAttempt(), execution.getRecoveryPointId(),
                execution.getRetentionPolicy(), execution.getTriggerType(), execution.getStatus(),
                execution.getDatabaseStatus(), execution.getRestrictionsStatus(), execution.getStorageStatus(),
                execution.getProtectedSizeBytes() == null ? 0L : execution.getProtectedSizeBytes(),
                execution.getProtectedFileCount() == null ? 0L : execution.getProtectedFileCount(),
                execution.getCompressionAlgorithm(), execution.getEncryptionAlgorithm(),
                execution.isDatabaseRestoreVerified(), execution.getDatabaseRestoreDurationSeconds(),
                execution.getRecoveryStatus(), execution.isIntegralRecoveryVerified(),
                execution.getIntegralRecoveryDurationSeconds(), execution.getIntegralRecoveryTestedAt(),
                execution.getStartedAt(), execution.getFinishedAt(), execution.getCommitSha(), execution.getWorkflowUrl());
    }
}
