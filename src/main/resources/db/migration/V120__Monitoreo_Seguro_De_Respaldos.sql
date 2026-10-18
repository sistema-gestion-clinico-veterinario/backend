CREATE TABLE IF NOT EXISTS backup_execution (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(20) NOT NULL,
    run_attempt INTEGER NOT NULL,
    recovery_point_id UUID,
    retention_policy VARCHAR(3),
    trigger_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    database_status VARCHAR(16) NOT NULL,
    restrictions_status VARCHAR(16) NOT NULL,
    storage_status VARCHAR(16) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    commit_sha VARCHAR(40),
    workflow_url VARCHAR(500) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_backup_execution_run UNIQUE (run_id, run_attempt),
    CONSTRAINT ck_backup_run_attempt CHECK (run_attempt > 0),
    CONSTRAINT ck_backup_retention CHECK (retention_policy IS NULL OR retention_policy IN ('p01', 'p02', 'p03')),
    CONSTRAINT ck_backup_trigger CHECK (trigger_type IN ('SCHEDULED', 'MANUAL')),
    CONSTRAINT ck_backup_status CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'PARTIAL', 'FAILED')),
    CONSTRAINT ck_backup_database_status CHECK (database_status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_backup_restrictions_status CHECK (restrictions_status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_backup_storage_status CHECK (storage_status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_backup_finished CHECK (
        (status = 'IN_PROGRESS' AND finished_at IS NULL)
        OR (status <> 'IN_PROGRESS' AND finished_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_backup_execution_started_at ON backup_execution (started_at DESC);
