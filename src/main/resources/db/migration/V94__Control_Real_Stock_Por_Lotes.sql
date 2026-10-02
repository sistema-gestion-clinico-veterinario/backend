ALTER TABLE producto
    ADD COLUMN control_stock VARCHAR(20) NOT NULL DEFAULT 'DIRECTO';

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_control_stock
    CHECK (control_stock IN ('DIRECTO', 'LOTES'));

ALTER TABLE lote
    ADD COLUMN cantidad_inicial INTEGER;

UPDATE lote SET cantidad_inicial = cantidad WHERE cantidad_inicial IS NULL;

ALTER TABLE lote
    ALTER COLUMN cantidad_inicial SET NOT NULL;

ALTER TABLE lote
    ADD CONSTRAINT chk_lote_cantidades
    CHECK (cantidad_inicial >= 0 AND cantidad >= 0 AND cantidad <= cantidad_inicial);

CREATE TABLE salida_lote (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    lote_id BIGINT NOT NULL REFERENCES lote(id),
    referencia_tipo VARCHAR(40) NOT NULL,
    referencia_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    cantidad_repuesta INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_repuesta >= 0 AND cantidad_repuesta <= cantidad),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

CREATE INDEX idx_salida_lote_referencia ON salida_lote(referencia_tipo, referencia_id);
CREATE INDEX idx_salida_lote_lote ON salida_lote(lote_id);
