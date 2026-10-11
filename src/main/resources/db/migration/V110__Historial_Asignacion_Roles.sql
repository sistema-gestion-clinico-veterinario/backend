-- Evidencia de quién concedió o retiró un rol. El historial es inmutable y guarda
-- instantáneas del nombre y la empresa, por eso no referencia con llaves foráneas a usuario, roles ni company:
-- eliminar o renombrar un rol no debe alterar el pasado.
CREATE TABLE IF NOT EXISTS role_assignment_history (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL,
    rol_id INTEGER,
    role_name VARCHAR(100) NOT NULL,
    company_id INTEGER,
    role_scope VARCHAR(20) NOT NULL,
    action VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    performed_by_user_id INTEGER,
    performed_by_email VARCHAR(254),
    CONSTRAINT ck_role_assignment_history_scope CHECK (role_scope IN ('PLATFORM', 'STAFF', 'CLIENT')),
    CONSTRAINT ck_role_assignment_history_action CHECK (action IN ('ASSIGNED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_role_assignment_history_usuario
    ON role_assignment_history (usuario_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_role_assignment_history_company
    ON role_assignment_history (company_id, occurred_at DESC);

-- Fecha y autor de cada asignación vigente. Las asignaciones anteriores a esta migración no tienen
-- fecha real registrada: se les asigna la fecha de la migración y quedan sin autor.
ALTER TABLE usuario_por_rol ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE usuario_por_rol ADD COLUMN IF NOT EXISTS created_by_user_id INTEGER;

UPDATE usuario_por_rol SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE usuario_por_rol ALTER COLUMN created_at SET NOT NULL;
