package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.BackupExecution;
import veterinaria.vargasvet.domain.enums.BackupComponentStatus;
import veterinaria.vargasvet.domain.enums.BackupEventPhase;
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
        execution.setUpdatedAt(AppClock.instantNow());
        repository.save(execution);
    }

    @Transactional(readOnly = true)
    public Page<BackupExecutionResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return repository.findAllByOrderByStartedAtDesc(PageRequest.of(safePage, safeSize)).map(this::toResponse);
    }

    private BackupExecutionResponse toResponse(BackupExecution execution) {
        return new BackupExecutionResponse(
                execution.getId(), execution.getRunId(), execution.getRunAttempt(), execution.getRecoveryPointId(),
                execution.getRetentionPolicy(), execution.getTriggerType(), execution.getStatus(),
                execution.getDatabaseStatus(), execution.getRestrictionsStatus(), execution.getStorageStatus(),
                execution.getDatabaseStatus() == BackupComponentStatus.SUCCESS,
                execution.getStartedAt(), execution.getFinishedAt(), execution.getCommitSha(), execution.getWorkflowUrl());
    }
}
