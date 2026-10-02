DROP INDEX IF EXISTS idx_lote_producto_numero;

CREATE UNIQUE INDEX idx_lote_producto_numero_ci
    ON lote (producto_id, LOWER(numero_lote));

COMMENT ON INDEX idx_lote_producto_numero_ci IS
    'Evita registrar dos veces el mismo número de lote para un producto, sin distinguir mayúsculas y minúsculas.';
