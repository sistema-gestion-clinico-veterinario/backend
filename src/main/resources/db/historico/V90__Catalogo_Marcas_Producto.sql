CREATE TABLE marca_producto (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(300),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uq_marca_producto_company_nombre
    ON marca_producto (company_id, LOWER(nombre));
CREATE INDEX idx_marca_producto_company_id
    ON marca_producto (company_id);

INSERT INTO marca_producto (company_id, nombre, descripcion, activo, created_at, created_by, updated_at, updated_by)
SELECT DISTINCT ON (company_id, LOWER(TRIM(marca)))
       company_id,
       TRIM(marca),
       'Marca migrada desde el catálogo de productos',
       TRUE,
       CURRENT_TIMESTAMP,
       'MIGRACION_V90',
       CURRENT_TIMESTAMP,
       'MIGRACION_V90'
  FROM producto
 WHERE marca IS NOT NULL
   AND TRIM(marca) <> ''
 ORDER BY company_id, LOWER(TRIM(marca)), id;

ALTER TABLE producto
    ADD COLUMN marca_id BIGINT REFERENCES marca_producto(id);

UPDATE producto producto_actual
   SET marca_id = marca_catalogo.id
  FROM marca_producto marca_catalogo
 WHERE marca_catalogo.company_id = producto_actual.company_id
   AND LOWER(marca_catalogo.nombre) = LOWER(TRIM(producto_actual.marca));

CREATE INDEX idx_producto_marca_id ON producto(marca_id);
