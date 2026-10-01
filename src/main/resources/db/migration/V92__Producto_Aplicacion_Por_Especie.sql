ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS aplicacion_especie VARCHAR(30) NOT NULL DEFAULT 'NO_ESPECIFICADO';

CREATE TABLE IF NOT EXISTS producto_especie (
    producto_id BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    especie VARCHAR(30) NOT NULL,
    CONSTRAINT pk_producto_especie PRIMARY KEY (producto_id, especie),
    CONSTRAINT chk_producto_especie_valida CHECK (
        especie IN ('PERRO', 'GATO', 'AVE', 'REPTIL', 'ROEDOR', 'EXOTICO', 'OTRO')
    )
);

CREATE INDEX IF NOT EXISTS idx_producto_especie_especie
    ON producto_especie (especie);

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_aplicacion_especie
    CHECK (aplicacion_especie IN ('USO_GENERAL', 'ESPECIES_ESPECIFICAS', 'NO_ESPECIFICADO'));

COMMENT ON COLUMN producto.aplicacion_especie IS
    'USO_GENERAL para insumos no dependientes de una especie; ESPECIES_ESPECIFICAS para productos dirigidos a una o varias especies; NO_ESPECIFICADO identifica registros anteriores pendientes de clasificar.';
