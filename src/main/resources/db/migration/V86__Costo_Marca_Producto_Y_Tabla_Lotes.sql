ALTER TABLE producto
    ADD COLUMN costo NUMERIC(10,2),
    ADD COLUMN marca VARCHAR(80);

CREATE TABLE lote (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    numero_lote VARCHAR(60) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_ingreso DATE,
    cantidad INTEGER NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(10,2),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_lote_company_id ON lote(company_id);
CREATE INDEX idx_lote_producto_id ON lote(producto_id);
CREATE INDEX idx_lote_fecha_vencimiento ON lote(fecha_vencimiento);
CREATE UNIQUE INDEX idx_lote_producto_numero ON lote(producto_id, numero_lote);
