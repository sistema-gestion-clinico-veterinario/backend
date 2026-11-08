ALTER TABLE clinical_template
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS applicable_species VARCHAR(300),
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE clinical_template
SET is_default = TRUE
WHERE template_key = 'GENERAL_CONSULTATION';

CREATE TABLE IF NOT EXISTS clinical_template_service (
    template_id BIGINT NOT NULL REFERENCES clinical_template(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES servicios(id) ON DELETE CASCADE,
    PRIMARY KEY (template_id, service_id)
);

CREATE INDEX IF NOT EXISTS idx_clinical_template_service_service
    ON clinical_template_service (service_id, template_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_clinical_template_default_published
    ON clinical_template (company_id)
    WHERE status = 'PUBLISHED' AND is_default = TRUE AND active = TRUE;
