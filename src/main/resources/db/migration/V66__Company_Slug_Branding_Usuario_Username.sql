-- Login por slug de empresa + username, con marca dinamica.
-- Aditiva + backfill en una sola migracion (el sistema no esta en produccion
-- todavia, no hay datos reales que migrar con cuidado extremo).
-- Ver C:\Users\Usuario\.claude\plans\proud-floating-nest.md para el plan completo.
--
-- No se usa la extension unaccent (requiere permisos que algunos hosting
-- administrados no dan por defecto) - se reemplazan acentos comunes del
-- espanol a mano, mas portatil.

ALTER TABLE company
    ADD COLUMN slug VARCHAR(100),
    ADD COLUMN color_primario VARCHAR(7);

ALTER TABLE usuario
    ADD COLUMN username VARCHAR(50);

-- Backfill de slug: minusculas, sin acentos, espacios/simbolos -> guion,
-- guiones repetidos colapsados, sin guion al inicio/fin. Colisiones (ej.
-- "Clínica..." vs "Clinica..." generan el mismo slug base) se resuelven
-- agregando el id como sufijo a partir de la segunda ocurrencia.
WITH slug_base AS (
    SELECT
        id,
        regexp_replace(
            regexp_replace(
                trim(
                    lower(
                        replace(replace(replace(replace(replace(
                        replace(replace(replace(replace(replace(
                        replace(replace(
                            name,
                        'á','a'),'é','e'),'í','i'),'ó','o'),'ú','u'),
                        'Á','a'),'É','e'),'Í','i'),'Ó','o'),'Ú','u'),
                        'ñ','n'),'Ñ','n')
                    )
                ),
                '[^a-z0-9]+', '-', 'g'
            ),
            '(^-+|-+$)', '', 'g'
        ) AS base
    FROM company
),
slug_ranked AS (
    SELECT
        id,
        base,
        ROW_NUMBER() OVER (PARTITION BY base ORDER BY id) AS rn
    FROM slug_base
)
UPDATE company c
SET slug = CASE WHEN sr.rn = 1 THEN sr.base ELSE sr.base || '-' || sr.id END
FROM slug_ranked sr
WHERE c.id = sr.id;

ALTER TABLE company
    ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX uq_company_slug ON company (slug);

-- Backfill de username: prefijo del correo (antes de @), mismo saneo que
-- el slug, colision resuelta con sufijo del id.
WITH username_base AS (
    SELECT
        id,
        regexp_replace(
            regexp_replace(
                trim(lower(split_part(email, '@', 1))),
                '[^a-z0-9._-]+', '', 'g'
            ),
            '(^[._-]+|[._-]+$)', '', 'g'
        ) AS base
    FROM usuario
),
username_ranked AS (
    SELECT
        id,
        base,
        ROW_NUMBER() OVER (PARTITION BY base ORDER BY id) AS rn
    FROM username_base
)
UPDATE usuario u
SET username = CASE WHEN ur.rn = 1 THEN ur.base ELSE ur.base || ur.id END
FROM username_ranked ur
WHERE u.id = ur.id;

ALTER TABLE usuario
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX uq_usuario_username ON usuario (username);

COMMENT ON COLUMN company.slug IS 'Identificador en la URL (systemvet.com/<slug>/login). Resuelve la empresa antes del login, sin pantalla de seleccion.';
COMMENT ON COLUMN company.color_primario IS 'Color de marca (hex, ej. #006BA8) para el login dinamico de esa empresa.';
COMMENT ON COLUMN usuario.username IS 'Identificador de login, unico globalmente. El correo deja de ser unico y de usarse para autenticar.';
