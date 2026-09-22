-- Contraseña independiente por (usuario, empresa). Antes, Usuario.password era una sola
-- contraseña global compartida entre todas las relaciones empresariales de la persona.
-- A partir de ahora cada relación (empleado o apoderado) con una empresa tiene su propia
-- credencial. SuperAdmin (sin empresa) usa company_id = NULL.

CREATE TABLE usuario_empresa_credencial (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    company_id INTEGER REFERENCES company(id),
    password VARCHAR(255) NOT NULL,
    password_changed BOOLEAN NOT NULL DEFAULT FALSE,
    credentials_version BIGINT NOT NULL DEFAULT 0,
    ultimo_acceso TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

-- Como mucho una credencial por (usuario, empresa); y como mucho una credencial "global"
-- (company_id NULL, SuperAdmin) por usuario. NULL no se compara como igual a otro NULL en
-- un UNIQUE normal, por eso se usan dos índices únicos parciales.
CREATE UNIQUE INDEX uq_credencial_usuario_empresa
    ON usuario_empresa_credencial (usuario_id, company_id)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX uq_credencial_usuario_global
    ON usuario_empresa_credencial (usuario_id)
    WHERE company_id IS NULL;

CREATE INDEX idx_credencial_usuario ON usuario_empresa_credencial (usuario_id);

-- Backfill: una fila por cada (usuario, empresa) donde la persona ya tiene relación de
-- empleado, copiando su contraseña ACTUAL como punto de partida (nadie pierde acceso).
INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, created_at)
SELECT DISTINCT u.id, e.company_id, u.password, u.password_changed, u.credentials_version, now()
FROM usuario u
JOIN empleado e ON e.user_id = u.id
WHERE e.company_id IS NOT NULL
ON CONFLICT (usuario_id, company_id) WHERE company_id IS NOT NULL DO NOTHING;

-- Igual para relaciones de apoderado (puede haber varias activas a la vez).
INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, created_at)
SELECT DISTINCT u.id, a.company_id, u.password, u.password_changed, u.credentials_version, now()
FROM usuario u
JOIN apoderado a ON a.user_id = u.id
WHERE a.company_id IS NOT NULL
ON CONFLICT (usuario_id, company_id) WHERE company_id IS NOT NULL DO NOTHING;

-- SuperAdmin u otros roles sin empresa (usuario_por_rol.company_id IS NULL): una credencial
-- "global" con company_id = NULL.
INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, created_at)
-- NULL::INTEGER explícito: con SELECT DISTINCT, Postgres necesita resolver el tipo del
-- literal NULL para poder comparar filas, y sin cast lo resuelve como "text" en vez de
-- heredarlo de la columna destino (company_id INTEGER) - eso rompe el INSERT.
SELECT DISTINCT u.id, NULL::INTEGER, u.password, u.password_changed, u.credentials_version, now()
FROM usuario u
JOIN usuario_por_rol upr ON upr.usuario_id = u.id
WHERE upr.company_id IS NULL
ON CONFLICT (usuario_id) WHERE company_id IS NULL DO NOTHING;

-- Red de seguridad: cualquier usuario que por alguna razón no haya quedado cubierto por
-- ninguna de las tres inserciones anteriores (sin empleado, sin apoderado, sin rol global)
-- igual conserva su contraseña actual como credencial global, para no dejar a nadie sin
-- forma de autenticar tras la migración.
INSERT INTO usuario_empresa_credencial (usuario_id, company_id, password, password_changed, credentials_version, created_at)
SELECT u.id, NULL, u.password, u.password_changed, u.credentials_version, now()
FROM usuario u
WHERE NOT EXISTS (SELECT 1 FROM usuario_empresa_credencial c WHERE c.usuario_id = u.id);
