-- Script local para ejecución manual en Supabase.
-- Evita carreras entre dos solicitudes que intenten crear el mismo rol.

CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_empresa_nombre
    ON roles (company_id, UPPER(name))
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_plataforma_nombre
    ON roles (UPPER(name))
    WHERE company_id IS NULL;
