ALTER TABLE purchases ADD COLUMN company_id INTEGER REFERENCES company(id);
ALTER TABLE purchases ADD COLUMN numero_venta VARCHAR(40);

CREATE INDEX idx_purchases_company_id ON purchases(company_id);

CREATE TABLE venta_libre_detalle (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES purchases(id),
    producto_id BIGINT NOT NULL REFERENCES producto(id),
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(10,2) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_venta_libre_detalle_purchase_id ON venta_libre_detalle(purchase_id);
