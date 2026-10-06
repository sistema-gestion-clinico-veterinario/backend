-- Fase 1 del plan de separacion identidad/empresa: migracion puramente aditiva.
-- No modifica ninguna constraint existente ni cambia comportamiento de la app.
-- Ver C:\Users\Usuario\.claude\plans\hidden-purring-abelson.md para el plan completo.

ALTER TABLE empleado
    ADD COLUMN company_id INTEGER NULL REFERENCES company (id),
    ADD COLUMN fecha_ingreso DATE NULL,
    ADD COLUMN fecha_salida DATE NULL;

ALTER TABLE apoderado
    ADD COLUMN company_id INTEGER NULL REFERENCES company (id),
    ADD COLUMN fecha_ingreso DATE NULL,
    ADD COLUMN fecha_salida DATE NULL,
    ADD COLUMN estado BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE usuario_por_rol
    ADD COLUMN company_id INTEGER NULL REFERENCES company (id);

-- Cubre el caso borde de un Usuario que no es ni Empleado ni Apoderado
-- (por ejemplo, una cuenta COMPANY_ADMIN pura sin perfil clinico).
CREATE TABLE usuario_membresia (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario (id),
    company_id INTEGER NOT NULL REFERENCES company (id),
    fecha_ingreso DATE NULL,
    fecha_salida DATE NULL,
    estado BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuario_membresia_usuario_id ON usuario_membresia (usuario_id);
CREATE INDEX idx_usuario_membresia_company_id ON usuario_membresia (company_id);
CREATE INDEX idx_empleado_company_id ON empleado (company_id);
CREATE INDEX idx_apoderado_company_id ON apoderado (company_id);
CREATE INDEX idx_usuario_por_rol_company_id ON usuario_por_rol (company_id);

COMMENT ON COLUMN empleado.company_id IS 'Empresa de esta relacion laboral especifica. Empleado pasa a ser el registro de membresia por empresa (junto con fecha_ingreso/fecha_salida/estado). Fuente de verdad: CompanyMembershipService, no Usuario.company.';
COMMENT ON COLUMN apoderado.company_id IS 'Empresa de esta relacion cliente especifica. A diferencia de Empleado, un mismo usuario puede tener varias filas Apoderado activas simultaneamente (cliente de varias empresas a la vez).';
COMMENT ON TABLE usuario_membresia IS 'Relacion empresa-usuario para cuentas sin perfil Empleado ni Apoderado (ej. COMPANY_ADMIN puro). Fallback usado por CompanyMembershipService.';
