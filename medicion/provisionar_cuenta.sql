\set ON_ERROR_STOP on

SELECT set_config('softvet.measurement_company_slug', :'measurement_company_slug', false) AS configured_company_slug
\gset
SELECT set_config('softvet.measurement_email', :'measurement_email', false) AS configured_email
\gset
SELECT set_config('softvet.measurement_password', :'measurement_password', false) AS configured_password
\gset
SELECT set_config('softvet.measurement_username', :'measurement_username', false) AS configured_username
\gset

DO $provision$
DECLARE
    target_company_id integer;
    target_user_id integer;
    admin_role_id integer;
BEGIN
    SELECT id
      INTO target_company_id
      FROM company
     WHERE slug = current_setting('softvet.measurement_company_slug')
       AND activo = true;

    IF target_company_id IS NULL THEN
        RAISE EXCEPTION 'No existe una clínica activa con el slug indicado';
    END IF;

    SELECT id
      INTO admin_role_id
      FROM roles
     WHERE name = 'ROLE_ADMIN'
       AND activo = true
     ORDER BY company_id NULLS FIRST, id
     LIMIT 1;

    IF admin_role_id IS NULL THEN
        RAISE EXCEPTION 'No existe un ROLE_ADMIN activo';
    END IF;

    INSERT INTO usuario (
        activo,
        apellido,
        company_id,
        credentials_version,
        email,
        email_verified,
        nombre,
        password_changed,
        username
    )
    VALUES (
        true,
        'Medición',
        target_company_id,
        0,
        current_setting('softvet.measurement_email'),
        true,
        'SoftVet',
        true,
        current_setting('softvet.measurement_username')
    )
    ON CONFLICT (username) DO UPDATE
       SET activo = true,
           company_id = EXCLUDED.company_id,
           email = EXCLUDED.email,
           email_verified = true,
           nombre = EXCLUDED.nombre,
           apellido = EXCLUDED.apellido,
           password_changed = true
    RETURNING id INTO target_user_id;

    INSERT INTO usuario_empresa_credencial (
        activada_con_google,
        company_id,
        created_at,
        credentials_version,
        password,
        password_changed,
        updated_at,
        usuario_id
    )
    VALUES (
        false,
        target_company_id,
        now(),
        0,
        crypt(current_setting('softvet.measurement_password'), gen_salt('bf', 12)),
        true,
        now(),
        target_user_id
    )
    ON CONFLICT (usuario_id, company_id) WHERE company_id IS NOT NULL
    DO UPDATE
       SET password = EXCLUDED.password,
           password_changed = true,
           credentials_version = usuario_empresa_credencial.credentials_version + 1,
           updated_at = now();

    UPDATE usuario_membresia
       SET estado = true,
           fecha_salida = NULL,
           updated_at = now()
     WHERE usuario_id = target_user_id
       AND company_id = target_company_id;

    IF NOT FOUND THEN
        INSERT INTO usuario_membresia (
            company_id,
            created_at,
            estado,
            fecha_ingreso,
            updated_at,
            usuario_id
        )
        VALUES (
            target_company_id,
            now(),
            true,
            current_date,
            now(),
            target_user_id
        );
    END IF;

    INSERT INTO usuario_por_rol (
        company_id,
        created_at,
        rol_id,
        usuario_id
    )
    VALUES (
        target_company_id,
        now(),
        admin_role_id,
        target_user_id
    )
    ON CONFLICT (usuario_id, rol_id) DO NOTHING;

    INSERT INTO user_consent (
        contenido_hash,
        documento_tipo,
        documento_version,
        fecha_aceptacion,
        ip_address,
        legal_document_id,
        texto_recuperable,
        tipo_constancia,
        user_agent,
        usuario_id
    )
    SELECT legal.contenido_hash,
           legal.tipo,
           legal.version,
           now(),
           '127.0.0.1',
           legal.id,
           true,
           CASE
               WHEN legal.tipo = 'TERMINOS_Y_CONDICIONES' THEN 'ACEPTACION'
               ELSE 'CONSTANCIA_LECTURA'
           END,
           'softvet-measurement-provisioner',
           target_user_id
      FROM legal_document legal
     WHERE legal.activo = true
    ON CONFLICT (usuario_id, legal_document_id) DO NOTHING;
END
$provision$;

SELECT 'Cuenta técnica de medición aprovisionada' AS resultado;
