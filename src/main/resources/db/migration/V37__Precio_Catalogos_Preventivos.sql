-- V37: Precio en el catalogo de vacunas y nuevo catalogo de desparasitantes.
-- El cobro de una aplicacion preventiva ahora se calcula del precio de la
-- vacuna/desparasitante (reemplaza al precio del servicio).
--
-- NOTA: ddl-auto=update no relaja constraints ni anade columnas NOT NULL sobre
-- tablas con datos. Ejecuta este script manualmente (como V35/V36). Idempotente.

DO $$
BEGIN
    -- 1) Precio en tipo_vacuna
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='tipo_vacuna' AND column_name='precio') THEN
        ALTER TABLE tipo_vacuna ADD COLUMN precio NUMERIC(10,2) DEFAULT 0 NOT NULL;
    END IF;

    -- 2) Tabla tipo_desparasitante
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='tipo_desparasitante') THEN
        CREATE TABLE tipo_desparasitante (
            id BIGSERIAL PRIMARY KEY,
            company_id BIGINT NOT NULL REFERENCES company (id),
            nombre VARCHAR(100) NOT NULL,
            especie VARCHAR(30) NOT NULL,
            periodicidad_meses_sugerida INT,
            precio NUMERIC(10,2) NOT NULL,
            activo BOOLEAN NOT NULL DEFAULT TRUE,
            created_at TIMESTAMP NOT NULL,
            created_by VARCHAR(150) NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            updated_by VARCHAR(150) NOT NULL,
            CONSTRAINT uk_tipo_desparasitante_company_nombre_especie
                UNIQUE (company_id, nombre, especie)
        );
    END IF;
END $$;