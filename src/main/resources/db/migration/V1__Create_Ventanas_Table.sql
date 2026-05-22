-- Create ventanas (modules/windows) table
CREATE TABLE ventanas (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(255) NOT NULL,
    grupo VARCHAR(100),
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ventanas_codigo ON ventanas(codigo);
CREATE INDEX idx_ventanas_activo ON ventanas(activo);
CREATE INDEX idx_ventanas_orden ON ventanas(orden);
