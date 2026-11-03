ALTER TABLE backup_execution
    ADD COLUMN IF NOT EXISTS protected_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS protected_file_count BIGINT,
    ADD COLUMN IF NOT EXISTS compression_algorithm VARCHAR(100),
    ADD COLUMN IF NOT EXISTS encryption_algorithm VARCHAR(50),
    ADD COLUMN IF NOT EXISTS database_restore_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS database_restore_duration_seconds BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_backup_protected_size_nonnegative'
          AND conrelid = 'backup_execution'::regclass
    ) THEN
        ALTER TABLE backup_execution
            ADD CONSTRAINT ck_backup_protected_size_nonnegative
                CHECK (protected_size_bytes IS NULL OR protected_size_bytes >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_backup_file_count_nonnegative'
          AND conrelid = 'backup_execution'::regclass
    ) THEN
        ALTER TABLE backup_execution
            ADD CONSTRAINT ck_backup_file_count_nonnegative
                CHECK (protected_file_count IS NULL OR protected_file_count >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_backup_restore_duration_nonnegative'
          AND conrelid = 'backup_execution'::regclass
    ) THEN
        ALTER TABLE backup_execution
            ADD CONSTRAINT ck_backup_restore_duration_nonnegative
                CHECK (database_restore_duration_seconds IS NULL OR database_restore_duration_seconds >= 0);
    END IF;
END
$$;
