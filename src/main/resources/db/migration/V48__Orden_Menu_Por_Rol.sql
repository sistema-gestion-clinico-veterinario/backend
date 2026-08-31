-- El orden visual es configuración del rol y no forma parte de sus permisos CRUD.
CREATE TABLE IF NOT EXISTS rol_vista_configuracion (
    id SERIAL PRIMARY KEY,
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    vista_id INTEGER NOT NULL REFERENCES vistas(id) ON DELETE CASCADE,
    orden INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_rol_vista_configuracion UNIQUE (rol_id, vista_id),
    CONSTRAINT ck_rol_vista_configuracion_orden CHECK (orden >= 0)
);

CREATE INDEX IF NOT EXISTS idx_rol_vista_configuracion_rol
    ON rol_vista_configuracion (rol_id);

INSERT INTO rol_vista_configuracion (rol_id, vista_id, orden)
SELECT permiso.rol_id, permiso.vista_id, COALESCE(vista.orden, 0)
FROM rol_vista_permisos permiso
JOIN vistas vista ON vista.id = permiso.vista_id
WHERE permiso.leer = TRUE
ON CONFLICT (rol_id, vista_id) DO NOTHING;
