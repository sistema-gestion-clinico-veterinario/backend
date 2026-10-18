package veterinaria.vargasvet.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import veterinaria.vargasvet.domain.enums.BackupComponentStatus;
import veterinaria.vargasvet.domain.enums.BackupRunStatus;
import veterinaria.vargasvet.domain.enums.BackupTriggerType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "backup_execution", uniqueConstraints =
        @UniqueConstraint(name = "uq_backup_execution_run", columnNames = {"run_id", "run_attempt"}))
public class BackupExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 20)
    private String runId;

    @Column(name = "run_attempt", nullable = false)
    private int runAttempt;

    @Column(name = "recovery_point_id")
    private UUID recoveryPointId;

    @Column(name = "retention_policy", length = 3)
    private String retentionPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 16)
    private BackupTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BackupRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_status", nullable = false, length = 16)
    private BackupComponentStatus databaseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "restrictions_status", nullable = false, length = 16)
    private BackupComponentStatus restrictionsStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_status", nullable = false, length = 16)
    private BackupComponentStatus storageStatus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(name = "workflow_url", nullable = false, length = 500)
    private String workflowUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
