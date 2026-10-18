package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.BackupExecution;

import java.util.Optional;

public interface BackupExecutionRepository extends JpaRepository<BackupExecution, Long> {
    Optional<BackupExecution> findByRunIdAndRunAttempt(String runId, int runAttempt);
    Page<BackupExecution> findAllByOrderByStartedAtDesc(Pageable pageable);
}
