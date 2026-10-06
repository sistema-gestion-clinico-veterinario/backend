ALTER TABLE rol_vista_permisos
    ADD COLUMN IF NOT EXISTS data_scope VARCHAR(16) NOT NULL DEFAULT 'OWN';

-- Conserva el comportamiento previo durante la migración: los roles que podían
-- modificar la agenda ya veían todos los registros. Desde ahora el alcance se
-- configura explícitamente y deja de depender del permiso MODIFICAR.
UPDATE rol_vista_permisos permission
SET data_scope = 'COMPANY'
FROM vistas view, roles role
WHERE permission.vista_id = view.id
  AND permission.rol_id = role.id
  AND view.codigo = 'VISTA_CITAS_AGENDA'
  AND (permission.modificar = TRUE OR role.purpose IN ('PLATFORM_ADMIN', 'COMPANY_ADMIN'));

ALTER TABLE rol_vista_permisos
    ADD CONSTRAINT chk_rol_vista_data_scope
    CHECK (data_scope IN ('OWN', 'COMPANY'));
