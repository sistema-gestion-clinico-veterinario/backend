CREATE TABLE ajuste_stock (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    diferencia INTEGER NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    observaciones VARCHAR(300),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX idx_ajuste_stock_company_id ON ajuste_stock(company_id);
CREATE INDEX idx_ajuste_stock_producto_id ON ajuste_stock(producto_id);
