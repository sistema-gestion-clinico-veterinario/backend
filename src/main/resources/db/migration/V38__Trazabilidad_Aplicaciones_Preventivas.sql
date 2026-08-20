-- Trazabilidad clinica y soporte de intervalos flexibles para la cartilla.

ALTER TABLE registro_vacunas
    ADD COLUMN IF NOT EXISTS intervalo_cantidad INT,
    ADD COLUMN IF NOT EXISTS intervalo_unidad VARCHAR(20),
    ADD COLUMN IF NOT EXISTS lote VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_vencimiento_producto DATE,
    ADD COLUMN IF NOT EXISTS dosis NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS unidad_dosis VARCHAR(30),
    ADD COLUMN IF NOT EXISTS via_administracion VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sitio_aplicacion VARCHAR(100),
    ADD COLUMN IF NOT EXISTS peso_kg NUMERIC(8,2),
    ADD COLUMN IF NOT EXISTS observaciones VARCHAR(500);

ALTER TABLE registro_desparasitaciones
    ADD COLUMN IF NOT EXISTS tipo_desparasitante_id BIGINT REFERENCES tipo_desparasitante(id),
    ADD COLUMN IF NOT EXISTS intervalo_cantidad INT,
    ADD COLUMN IF NOT EXISTS intervalo_unidad VARCHAR(20),
    ADD COLUMN IF NOT EXISTS lote VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_vencimiento_producto DATE,
    ADD COLUMN IF NOT EXISTS dosis NUMERIC(10,3),
    ADD COLUMN IF NOT EXISTS unidad_dosis VARCHAR(30),
    ADD COLUMN IF NOT EXISTS via_administracion VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sitio_aplicacion VARCHAR(100),
    ADD COLUMN IF NOT EXISTS peso_kg NUMERIC(8,2),
    ADD COLUMN IF NOT EXISTS observaciones VARCHAR(500);

ALTER TABLE registro_desparasitaciones
    ALTER COLUMN periodicidad_meses DROP NOT NULL,
    ALTER COLUMN fecha_proxima_aplicacion DROP NOT NULL;
