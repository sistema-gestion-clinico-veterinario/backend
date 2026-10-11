-- Cada clínica mantiene avisos independientes para clientes y para su personal.
-- Los avisos históricos existentes fueron redactados para propietarios de mascotas,
-- por eso se clasifican sin alterar su contenido ni sus constancias asociadas.
ALTER TABLE aviso_privacidad
    ADD COLUMN IF NOT EXISTS audiencia VARCHAR(40);

UPDATE aviso_privacidad
SET audiencia = 'PROPIETARIOS_Y_AUTORIZADOS'
WHERE audiencia IS NULL;

ALTER TABLE aviso_privacidad
    ALTER COLUMN audiencia SET NOT NULL;

ALTER TABLE aviso_privacidad
    DROP CONSTRAINT IF EXISTS ck_aviso_privacidad_audiencia;
ALTER TABLE aviso_privacidad
    ADD CONSTRAINT ck_aviso_privacidad_audiencia CHECK (
        audiencia IN ('PROPIETARIOS_Y_AUTORIZADOS', 'TRABAJADORES_Y_USUARIOS')
    );

ALTER TABLE aviso_privacidad
    DROP CONSTRAINT IF EXISTS uq_aviso_privacidad_company_version;
ALTER TABLE aviso_privacidad
    ADD CONSTRAINT uq_aviso_privacidad_company_audiencia_version
        UNIQUE (company_id, audiencia, version);

DROP INDEX IF EXISTS ux_aviso_privacidad_vigente;
CREATE UNIQUE INDEX ux_aviso_privacidad_vigente_audiencia
    ON aviso_privacidad (company_id, audiencia)
    WHERE activo;

CREATE OR REPLACE FUNCTION aviso_privacidad_inmutable() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los avisos de privacidad no se pueden eliminar';
    END IF;
    IF NEW.company_id IS DISTINCT FROM OLD.company_id
       OR NEW.audiencia IS DISTINCT FROM OLD.audiencia
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

COMMENT ON COLUMN aviso_privacidad.audiencia IS
    'Grupo de personas al que se informa; cada audiencia conserva versiones y vigencia independientes';
