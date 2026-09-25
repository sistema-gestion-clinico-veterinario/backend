CREATE TABLE categoria_producto (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    nombre VARCHAR(80) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categoria_producto_company_id ON categoria_producto(company_id);

CREATE TABLE producto (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    categoria_id BIGINT NOT NULL REFERENCES categoria_producto(id),
    nombre VARCHAR(160) NOT NULL,
    precio NUMERIC(10,2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    descripcion VARCHAR(300),
    imagen_url VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_producto_company_id ON producto(company_id);
CREATE INDEX idx_producto_company_activo ON producto(company_id, activo);
CREATE INDEX idx_producto_categoria_id ON producto(categoria_id);
