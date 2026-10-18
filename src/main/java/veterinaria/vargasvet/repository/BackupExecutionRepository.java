package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.BackupExecution;

import veterinaria.vargasvet.domain.enums.BackupRunStatus;

import java.time.Instant;
import java.util.Optional;

public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {
    Optional<BackupExecution> findByRunIdAndRunAttempt(String runId, int runAttempt);
    Page<BackupExecution> findAllByOrderByStartedAtDesc(Pageable pageable);
    Optional<BackupExecution> findFirstByOrderByStartedAtDesc();
    Optional<BackupExecution> findFirstByStatusOrderByStartedAtDesc(BackupRunStatus status);
    long countByStatusAndStartedAtAfter(BackupRunStatus status, Instant since);
}
