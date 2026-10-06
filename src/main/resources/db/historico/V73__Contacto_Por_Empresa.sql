-- Contacto (telefono, direccion) independiente por (usuario, empresa). Antes
-- Usuario.telefono/direccion era un solo valor global compartido entre todas las
-- relaciones empresariales de la persona - mismo problema que tenia la contraseña antes
-- de V70, misma solucion.

CREATE TABLE usuario_empresa_contacto (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    company_id INTEGER REFERENCES company(id),
    telefono VARCHAR(255),
    direccion VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

-- Como mucho un contacto por (usuario, empresa); y como mucho un contacto "global" (sin
-- empresa) por usuario. NULL no se compara como igual a otro NULL en un UNIQUE normal,
-- por eso se usan dos indices unicos parciales (mismo patron que V70).
CREATE UNIQUE INDEX uq_contacto_usuario_empresa
    ON usuario_empresa_contacto (usuario_id, company_id)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX uq_contacto_usuario_global
    ON usuario_empresa_contacto (usuario_id)
    WHERE company_id IS NULL;

CREATE INDEX idx_contacto_usuario ON usuario_empresa_contacto (usuario_id);

-- Backfill: una fila por cada (usuario, empresa) donde la persona ya tiene relacion de
-- empleado, copiando su telefono/direccion ACTUAL como punto de partida.
INSERT INTO usuario_empresa_contacto (usuario_id, company_id, telefono, direccion, created_at)
SELECT DISTINCT u.id, e.company_id, u.telefono, u.direccion, now()
FROM usuario u
JOIN empleado e ON e.user_id = u.id
WHERE e.company_id IS NOT NULL
ON CONFLICT (usuario_id, company_id) WHERE company_id IS NOT NULL DO NOTHING;

-- Igual para relaciones de apoderado (puede haber varias activas a la vez).
INSERT INTO usuario_empresa_contacto (usuario_id, company_id, telefono, direccion, created_at)
SELECT DISTINCT u.id, a.company_id, u.telefono, u.direccion, now()
FROM usuario u
JOIN apoderado a ON a.user_id = u.id
WHERE a.company_id IS NOT NULL
ON CONFLICT (usuario_id, company_id) WHERE company_id IS NOT NULL DO NOTHING;

-- SuperAdmin u otros roles sin empresa (usuario_por_rol.company_id IS NULL): un contacto
-- "global" con company_id = NULL. NULL::INTEGER explicito: con SELECT DISTINCT, Postgres
-- necesita resolver el tipo del literal NULL para poder comparar filas, y sin cast lo
-- resuelve como "text" en vez de heredarlo de la columna destino (company_id INTEGER).
INSERT INTO usuario_empresa_contacto (usuario_id, company_id, telefono, direccion, created_at)
SELECT DISTINCT u.id, NULL::INTEGER, u.telefono, u.direccion, now()
FROM usuario u
JOIN usuario_por_rol upr ON upr.usuario_id = u.id
WHERE upr.company_id IS NULL
ON CONFLICT (usuario_id) WHERE company_id IS NULL DO NOTHING;

-- Red de seguridad: cualquier usuario no cubierto por ninguna de las tres inserciones
-- anteriores igual conserva su telefono/direccion actual como contacto global.
INSERT INTO usuario_empresa_contacto (usuario_id, company_id, telefono, direccion, created_at)
SELECT u.id, NULL, u.telefono, u.direccion, now()
FROM usuario u
WHERE NOT EXISTS (SELECT 1 FROM usuario_empresa_contacto c WHERE c.usuario_id = u.id);
