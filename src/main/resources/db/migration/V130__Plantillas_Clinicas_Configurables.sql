CREATE TABLE clinical_concept (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER REFERENCES company(id),
    code VARCHAR(120) NOT NULL,
    base_name VARCHAR(160) NOT NULL,
    description TEXT,
    value_type VARCHAR(24) NOT NULL,
    unit VARCHAR(40),
    origin VARCHAR(16) NOT NULL,
    reportable BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    validation_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT ck_clinical_concept_origin CHECK (origin IN ('SYSTEM', 'COMPANY')),
    CONSTRAINT ck_clinical_concept_value_type CHECK (value_type IN (
        'TEXT_SHORT', 'TEXT_LONG', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE',
        'SELECT_ONE', 'SELECT_MULTIPLE', 'SCALE', 'FILE', 'DISPLAY'
    )),
    CONSTRAINT ck_clinical_concept_owner CHECK (
        (origin = 'SYSTEM' AND company_id IS NULL)
        OR (origin = 'COMPANY' AND company_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_clinical_concept_system_code
    ON clinical_concept (code) WHERE company_id IS NULL;
CREATE UNIQUE INDEX uk_clinical_concept_company_code
    ON clinical_concept (company_id, code) WHERE company_id IS NOT NULL;
CREATE INDEX idx_clinical_concept_company_active
    ON clinical_concept (company_id, active);

CREATE TABLE clinical_template (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    template_key VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    version INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    based_on_id BIGINT REFERENCES clinical_template(id),
    published_at TIMESTAMPTZ,
    published_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT uk_clinical_template_version UNIQUE (company_id, template_key, version),
    CONSTRAINT ck_clinical_template_version CHECK (version >= 1),
    CONSTRAINT ck_clinical_template_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_clinical_template_published
    ON clinical_template (company_id, template_key)
    WHERE status = 'PUBLISHED';
CREATE INDEX idx_clinical_template_company_status
    ON clinical_template (company_id, status);

CREATE TABLE clinical_template_section (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES clinical_template(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT uk_clinical_template_section_order UNIQUE (template_id, display_order),
    CONSTRAINT ck_clinical_template_section_order CHECK (display_order >= 0)
);

CREATE TABLE clinical_template_field (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES clinical_template_section(id) ON DELETE CASCADE,
    concept_id BIGINT NOT NULL REFERENCES clinical_concept(id),
    visible_label VARCHAR(160) NOT NULL,
    help_text VARCHAR(500),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL,
    applicable_species VARCHAR(300),
    visibility_rules JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uk_clinical_template_field_concept UNIQUE (section_id, concept_id),
    CONSTRAINT uk_clinical_template_field_order UNIQUE (section_id, display_order),
    CONSTRAINT ck_clinical_template_field_order CHECK (display_order >= 0)
);

ALTER TABLE consulta
    ADD COLUMN IF NOT EXISTS clinical_template_id BIGINT REFERENCES clinical_template(id);

CREATE TABLE consultation_field_answer (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL REFERENCES consulta(id) ON DELETE CASCADE,
    template_field_id BIGINT NOT NULL REFERENCES clinical_template_field(id),
    value_text TEXT,
    value_number NUMERIC(18,4),
    value_boolean BOOLEAN,
    value_date DATE,
    value_json JSONB,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recorded_by VARCHAR(255),
    CONSTRAINT uk_consultation_field_answer UNIQUE (consultation_id, template_field_id),
    CONSTRAINT ck_consultation_field_answer_single_value CHECK (
        num_nonnulls(value_text, value_number, value_boolean, value_date, value_json) <= 1
    )
);

CREATE INDEX idx_consultation_field_answer_consultation
    ON consultation_field_answer (consultation_id);
CREATE INDEX idx_consultation_field_answer_field_number
    ON consultation_field_answer (template_field_id, value_number)
    WHERE value_number IS NOT NULL;

INSERT INTO clinical_concept
    (code, base_name, description, value_type, unit, origin, reportable, validation_config)
VALUES
    ('consultation.reason', 'Motivo de consulta', 'Razón principal de la atención.', 'TEXT_LONG', NULL, 'SYSTEM', TRUE, '{"coreField":"motivoConsulta","maxLength":1000}'),
    ('consultation.anamnesis', 'Anamnesis', 'Historia y evolución informada durante la atención.', 'TEXT_LONG', NULL, 'SYSTEM', TRUE, '{"coreField":"anamnesis","maxLength":1000}'),
    ('vital.weight', 'Peso', 'Peso registrado durante la atención.', 'DECIMAL', 'kg', 'SYSTEM', TRUE, '{"coreField":"pesoEnConsulta","min":0.01,"max":120}'),
    ('vital.temperature', 'Temperatura', 'Temperatura corporal registrada durante la atención.', 'DECIMAL', '°C', 'SYSTEM', TRUE, '{"coreField":"temperatura","min":0.1,"max":45}'),
    ('vital.heart_rate', 'Frecuencia cardíaca', 'Frecuencia cardíaca durante la atención.', 'INTEGER', 'lpm', 'SYSTEM', TRUE, '{"coreField":"frecuenciaCardiaca","min":1,"max":300}'),
    ('vital.respiratory_rate', 'Frecuencia respiratoria', 'Frecuencia respiratoria durante la atención.', 'INTEGER', 'rpm', 'SYSTEM', TRUE, '{"coreField":"frecuenciaRespiratoria","min":1,"max":200}'),
    ('exam.mucosa', 'Mucosas', 'Descripción del estado de las mucosas.', 'TEXT_SHORT', NULL, 'SYSTEM', TRUE, '{"coreField":"mucosas","maxLength":80}'),
    ('exam.skin_turgor', 'Turgencia de piel', 'Descripción de la turgencia de la piel.', 'TEXT_SHORT', NULL, 'SYSTEM', TRUE, '{"coreField":"turgenciaPiel","maxLength":80}'),
    ('consultation.physical_exam', 'Examen físico', 'Hallazgos del examen físico.', 'TEXT_LONG', NULL, 'SYSTEM', TRUE, '{"coreField":"examenFisico","maxLength":1000}'),
    ('consultation.observations', 'Observaciones', 'Observaciones adicionales de la atención.', 'TEXT_LONG', NULL, 'SYSTEM', FALSE, '{"coreField":"observaciones","maxLength":500}')
ON CONFLICT DO NOTHING;

INSERT INTO clinical_template
    (company_id, template_key, name, version, status, published_at, published_by, created_by)
SELECT c.id, 'GENERAL_CONSULTATION', 'Ficha clínica general', 1, 'PUBLISHED',
       CURRENT_TIMESTAMP, 'SYSTEM', 'SYSTEM'
FROM company c
WHERE NOT EXISTS (
    SELECT 1 FROM clinical_template t
    WHERE t.company_id = c.id AND t.template_key = 'GENERAL_CONSULTATION'
);

INSERT INTO clinical_template_section (template_id, title, display_order)
SELECT t.id, s.title, s.display_order
FROM clinical_template t
CROSS JOIN (VALUES
    ('Motivo y antecedentes', 0),
    ('Signos vitales', 1),
    ('Evaluación clínica', 2)
) AS s(title, display_order)
WHERE t.template_key = 'GENERAL_CONSULTATION'
  AND t.version = 1
  AND NOT EXISTS (
      SELECT 1 FROM clinical_template_section existing
      WHERE existing.template_id = t.id
  );

INSERT INTO clinical_template_field
    (section_id, concept_id, visible_label, help_text, required, display_order)
SELECT section.id, concept.id, mapping.label, mapping.help_text, mapping.required, mapping.display_order
FROM clinical_template_section section
JOIN clinical_template template ON template.id = section.template_id
JOIN (VALUES
    (0, 'consultation.reason', 'Motivo de consulta', 'Indique la razón principal de la atención.', TRUE, 0),
    (0, 'consultation.anamnesis', 'Anamnesis', 'Registre la historia y evolución informada.', TRUE, 1),
    (1, 'vital.weight', 'Peso', NULL, TRUE, 0),
    (1, 'vital.temperature', 'Temperatura', NULL, FALSE, 1),
    (1, 'vital.heart_rate', 'Frecuencia cardíaca', NULL, FALSE, 2),
    (1, 'vital.respiratory_rate', 'Frecuencia respiratoria', NULL, FALSE, 3),
    (2, 'exam.mucosa', 'Mucosas', NULL, FALSE, 0),
    (2, 'exam.skin_turgor', 'Turgencia de piel', NULL, FALSE, 1),
    (2, 'consultation.physical_exam', 'Examen físico', NULL, FALSE, 2),
    (2, 'consultation.observations', 'Observaciones', NULL, FALSE, 3)
) AS mapping(section_order, concept_code, label, help_text, required, display_order)
    ON mapping.section_order = section.display_order
JOIN clinical_concept concept ON concept.code = mapping.concept_code AND concept.company_id IS NULL
WHERE template.template_key = 'GENERAL_CONSULTATION'
  AND template.version = 1
ON CONFLICT DO NOTHING;

UPDATE consulta consultation
SET clinical_template_id = template.id
FROM historia_clinica history
JOIN mascota pet ON pet.id = history.mascota_id
JOIN apoderado owner ON owner.id = pet.apoderado_id
JOIN clinical_template template
  ON template.company_id = owner.company_id
 AND template.template_key = 'GENERAL_CONSULTATION'
 AND template.version = 1
WHERE consultation.historia_clinica_id = history.id
  AND consultation.clinical_template_id IS NULL;
