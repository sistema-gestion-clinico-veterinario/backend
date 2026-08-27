DO $$
DECLARE
    current_type text;
BEGIN
    SELECT data_type INTO current_type
    FROM information_schema.columns
    WHERE table_name = 'audit_logs' AND column_name = 'user_email';

    IF current_type = 'bytea' THEN
        ALTER TABLE audit_logs
            ALTER COLUMN user_email TYPE VARCHAR(255)
            USING convert_from(user_email, 'UTF8');
    ELSIF current_type IS NOT NULL THEN
        ALTER TABLE audit_logs
            ALTER COLUMN user_email TYPE VARCHAR(255)
            USING user_email::text;
    END IF;
END $$;
