ALTER TABLE tipo_vacuna
    ADD COLUMN IF NOT EXISTS lote VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_vencimiento_producto DATE,
    ADD COLUMN IF NOT EXISTS dosis NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS unidad_dosis VARCHAR(30),
    ADD COLUMN IF NOT EXISTS via_administracion VARCHAR(50);

ALTER TABLE tipo_desparasitante
    ADD COLUMN IF NOT EXISTS lote VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_vencimiento_producto DATE,
    ADD COLUMN IF NOT EXISTS dosis NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS unidad_dosis VARCHAR(30),
    ADD COLUMN IF NOT EXISTS via_administracion VARCHAR(50);
