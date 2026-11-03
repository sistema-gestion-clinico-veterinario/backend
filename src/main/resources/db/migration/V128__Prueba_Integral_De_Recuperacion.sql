ALTER TABLE backup_execution
    ADD COLUMN IF NOT EXISTS recovery_status VARCHAR(16) NOT NULL DEFAULT 'SKIPPED',
    ADD COLUMN IF NOT EXISTS integral_recovery_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS integral_recovery_duration_seconds BIGINT,
    ADD COLUMN IF NOT EXISTS integral_recovery_tested_at TIMESTAMPTZ;

ALTER TABLE backup_execution
    ADD CONSTRAINT ck_backup_integral_recovery_duration_nonnegative
        CHECK (integral_recovery_duration_seconds IS NULL OR integral_recovery_duration_seconds >= 0),
    ADD CONSTRAINT ck_backup_integral_recovery_status
        CHECK (recovery_status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED'));

CREATE INDEX IF NOT EXISTS idx_backup_execution_integral_recovery
    ON backup_execution (integral_recovery_verified, integral_recovery_tested_at DESC);
