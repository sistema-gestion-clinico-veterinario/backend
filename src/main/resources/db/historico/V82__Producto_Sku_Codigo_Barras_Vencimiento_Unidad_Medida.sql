CREATE TABLE unidad_medida (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    nombre VARCHAR(40) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_unidad_medida_company_id ON unidad_medida(company_id);

ALTER TABLE producto
    ADD COLUMN sku VARCHAR(30),
    ADD COLUMN codigo_barras VARCHAR(64),
    ADD COLUMN fecha_vencimiento DATE,
    ADD COLUMN requiere_receta BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN unidad_medida_id BIGINT REFERENCES unidad_medida(id);

WITH numerado AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY company_id ORDER BY id) AS rn
    FROM producto
)
UPDATE producto p
SET sku = 'PRD-' || LPAD(numerado.rn::text, 5, '0')
FROM numerado
WHERE p.id = numerado.id;

ALTER TABLE producto ALTER COLUMN sku SET NOT NULL;

CREATE UNIQUE INDEX idx_producto_company_sku ON producto(company_id, sku);
CREATE UNIQUE INDEX idx_producto_company_codigo_barras ON producto(company_id, codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE INDEX idx_producto_unidad_medida_id ON producto(unidad_medida_id);
