-- Constancia del dispositivo desde el que se publicó cada aviso de privacidad y del que se originó cada evento de auditoría.
ALTER TABLE aviso_privacidad
    ADD COLUMN IF NOT EXISTS creado_ip VARCHAR(64),
    ADD COLUMN IF NOT EXISTS creado_dispositivo VARCHAR(120);

-- La constancia forma parte del aviso publicado: queda protegida igual que el resto de sus datos.
CREATE OR REPLACE FUNCTION aviso_privacidad_inmutable() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los avisos de privacidad no se pueden eliminar';
    END IF;
    IF NEW.company_id IS DISTINCT FROM OLD.company_id
       OR NEW.version IS DISTINCT FROM OLD.version
       OR NEW.contenido IS DISTINCT FROM OLD.contenido
       OR NEW.contenido_hash IS DISTINCT FROM OLD.contenido_hash
       OR NEW.campos IS DISTINCT FROM OLD.campos
       OR NEW.vigente_desde IS DISTINCT FROM OLD.vigente_desde
       OR NEW.creado_en IS DISTINCT FROM OLD.creado_en
       OR NEW.creado_por IS DISTINCT FROM OLD.creado_por
       OR NEW.creado_ip IS DISTINCT FROM OLD.creado_ip
       OR NEW.creado_dispositivo IS DISTINCT FROM OLD.creado_dispositivo THEN
        RAISE EXCEPTION 'Un aviso de privacidad publicado no se puede modificar; publica una versión nueva';
    END IF;
    IF NEW.activo AND NOT OLD.activo THEN
        RAISE EXCEPTION 'Un aviso retirado no se puede volver a activar; publica una versión nueva';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS dispositivo VARCHAR(120);

COMMENT ON COLUMN aviso_privacidad.creado_dispositivo IS 'Navegador y sistema desde los que el administrador publicó esta versión';
COMMENT ON COLUMN audit_logs.dispositivo IS 'Navegador y sistema desde los que se originó el evento';
