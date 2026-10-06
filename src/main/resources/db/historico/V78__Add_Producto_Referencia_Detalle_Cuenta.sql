ALTER TABLE detalle_cuenta_cita ADD COLUMN producto_id BIGINT REFERENCES producto(id);

CREATE INDEX idx_detalle_cuenta_cita_producto_id ON detalle_cuenta_cita(producto_id);
