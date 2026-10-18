-- Los registros creados antes de incorporar @Version pueden conservar NULL.
-- Hibernate no puede incrementar una version nula al actualizar la entidad.

UPDATE citas SET version = 0 WHERE version IS NULL;
ALTER TABLE citas ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE citas ALTER COLUMN version SET NOT NULL;

UPDATE consulta SET version = 0 WHERE version IS NULL;
ALTER TABLE consulta ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE consulta ALTER COLUMN version SET NOT NULL;

UPDATE control_preventivo SET version = 0 WHERE version IS NULL;
ALTER TABLE control_preventivo ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE control_preventivo ALTER COLUMN version SET NOT NULL;

UPDATE registro_vacunas SET version = 0 WHERE version IS NULL;
ALTER TABLE registro_vacunas ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE registro_vacunas ALTER COLUMN version SET NOT NULL;

UPDATE registro_desparasitaciones SET version = 0 WHERE version IS NULL;
ALTER TABLE registro_desparasitaciones ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE registro_desparasitaciones ALTER COLUMN version SET NOT NULL;
