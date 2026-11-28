\set ON_ERROR_STOP on
\if :{?paso}
\else
\set paso 1
\endif

SELECT set_config('softvet.paso', :'paso', false) AS e \gset
SELECT set_config('softvet.slug', :'slug', false) AS a \gset
SELECT set_config('softvet.clave', :'clave', false) AS b \gset
SELECT set_config('softvet.desde', :'desde', false) AS c \gset
SELECT set_config('softvet.hasta', :'hasta', false) AS d \gset

DO $cuentas$
DECLARE
    empresa integer;
    rol_admin integer;
    hash_clave text := crypt(current_setting('softvet.clave'), gen_salt('bf', 12));
    i integer;
    usuario_id integer;
BEGIN
    SELECT id INTO empresa FROM company WHERE slug = current_setting('softvet.slug') AND activo = true;
    IF empresa IS NULL THEN
        RAISE EXCEPTION 'No existe una clínica activa con el slug indicado';
    END IF;

    SELECT id INTO rol_admin FROM roles WHERE name = 'ROLE_ADMIN' AND activo = true ORDER BY company_id NULLS FIRST, id LIMIT 1;
    IF rol_admin IS NULL THEN
        RAISE EXCEPTION 'No existe un ROLE_ADMIN activo';
    END IF;

    FOR i IN current_setting('softvet.desde')::integer .. current_setting('softvet.hasta')::integer BY current_setting('softvet.paso')::integer LOOP
        INSERT INTO usuario (activo, apellido, company_id, email, email_verified, nombre, username)
        VALUES (true, 'Medición', empresa, 'medicion' || i || '@softvet.test', true, 'SoftVet', 'medicion' || i)
        RETURNING id INTO usuario_id;

        INSERT INTO usuario_empresa_credencial (
            activada_con_google, company_id, created_at, credentials_version, password, password_changed, updated_at, usuario_id
        ) VALUES (false, empresa, now(), 0, hash_clave, true, now(), usuario_id);

        INSERT INTO usuario_membresia (company_id, created_at, estado, fecha_ingreso, updated_at, usuario_id)
        VALUES (empresa, now(), true, current_date, now(), usuario_id);

        INSERT INTO usuario_por_rol (company_id, created_at, rol_id, usuario_id)
        VALUES (empresa, now(), rol_admin, usuario_id);

        INSERT INTO user_consent (
            contenido_hash, documento_tipo, documento_version, fecha_aceptacion, ip_address,
            legal_document_id, texto_recuperable, tipo_constancia, user_agent, usuario_id
        )
        SELECT legal.contenido_hash, legal.tipo, legal.version, now(), '127.0.0.1', legal.id, true,
               CASE WHEN legal.tipo = 'TERMINOS_Y_CONDICIONES' THEN 'ACEPTACION' ELSE 'CONSTANCIA_LECTURA' END,
               'medicion-carga', usuario_id
          FROM legal_document legal
         WHERE legal.activo = true;
    END LOOP;
END
$cuentas$;

SELECT 'cuentas de medición creadas: ' || count(*) FROM usuario WHERE username LIKE 'medicion%';
