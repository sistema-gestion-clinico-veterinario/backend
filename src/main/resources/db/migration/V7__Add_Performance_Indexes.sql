-- Indices de rendimiento para listados, filtros y validaciones frecuentes.
-- PostgreSQL/Supabase: pg_trgm permite busquedas parciales y similares por texto.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Seguridad y autenticacion
CREATE INDEX IF NOT EXISTS idx_usuario_company_id
    ON usuario (company_id);

CREATE INDEX IF NOT EXISTS idx_usuario_company_apellido_nombre
    ON usuario (company_id, apellido, nombre);

CREATE INDEX IF NOT EXISTS idx_usuario_verification_token
    ON usuario (verification_token);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_usuario_expiry
    ON refresh_tokens (usuario_id, expiry_date DESC);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_usuario
    ON password_reset_tokens (usuario_id);

-- Relaciones base por empresa
CREATE INDEX IF NOT EXISTS idx_apoderado_user_id
    ON apoderado (user_id);

CREATE INDEX IF NOT EXISTS idx_apoderado_numero_documento
    ON apoderado (numero_documento);

CREATE INDEX IF NOT EXISTS idx_mascota_apoderado_id
    ON mascota (apoderado_id);

CREATE INDEX IF NOT EXISTS idx_mascota_raza_id
    ON mascota (raza_id);

CREATE INDEX IF NOT EXISTS idx_mascota_uuid
    ON mascota (uuid);

-- Historias clinicas y consultas
CREATE INDEX IF NOT EXISTS idx_historia_clinica_mascota_id
    ON historia_clinica (mascota_id);

CREATE INDEX IF NOT EXISTS idx_historia_clinica_numero_hc
    ON historia_clinica (numero_hc);

CREATE INDEX IF NOT EXISTS idx_consulta_historia_fecha
    ON consulta (historia_clinica_id, fecha_consulta DESC);

CREATE INDEX IF NOT EXISTS idx_consulta_cita_id
    ON consulta (cita_id);

CREATE INDEX IF NOT EXISTS idx_consulta_veterinario_fecha
    ON consulta (veterinario_id, fecha_consulta DESC);

-- Citas: agenda, cruces de horarios y dashboard
CREATE INDEX IF NOT EXISTS idx_citas_empleado_fecha_activa
    ON citas (empleado_id, fecha_hora_inicio, fecha_hora_fin)
    WHERE eliminada = false;

CREATE INDEX IF NOT EXISTS idx_citas_mascota_fecha_activa
    ON citas (mascota_id, fecha_hora_inicio, fecha_hora_fin)
    WHERE eliminada = false;

CREATE INDEX IF NOT EXISTS idx_citas_fecha_estado_activa
    ON citas (fecha_hora_inicio DESC, estado)
    WHERE eliminada = false;

CREATE INDEX IF NOT EXISTS idx_citas_servicio_id
    ON citas (servicio_id);

-- Caja y pagos internos
CREATE INDEX IF NOT EXISTS idx_movimiento_caja_company_fecha
    ON movimiento_caja (company_id, fecha DESC);

CREATE INDEX IF NOT EXISTS idx_movimiento_caja_company_tipo_fecha
    ON movimiento_caja (company_id, tipo, fecha);

CREATE INDEX IF NOT EXISTS idx_movimiento_caja_cita_tipo
    ON movimiento_caja (cita_id, tipo);

-- Horarios y disponibilidad
CREATE INDEX IF NOT EXISTS idx_horario_empleado_fecha
    ON horario_empleado (empleado_id, fecha);

CREATE INDEX IF NOT EXISTS idx_horario_empleado_fecha_horas
    ON horario_empleado (empleado_id, fecha, hora_inicio, hora_fin);

CREATE INDEX IF NOT EXISTS idx_company_exception_company_date
    ON company_exceptions (company_id, date);

CREATE INDEX IF NOT EXISTS idx_company_operating_hours_company_day
    ON company_operating_hours (company_id, dia_semana);

-- Catalogos por empresa
CREATE INDEX IF NOT EXISTS idx_servicios_company_nombre
    ON servicios (company_id, nombre);

CREATE INDEX IF NOT EXISTS idx_servicios_company_estado
    ON servicios (company_id, disponible, activo);

CREATE INDEX IF NOT EXISTS idx_especialidad_company_nombre
    ON especialidad (company_id, nombre);

CREATE INDEX IF NOT EXISTS idx_tipo_empleado_company_nombre
    ON tipo_empleado (company_id, nombre);

CREATE INDEX IF NOT EXISTS idx_raza_company_especie_nombre
    ON raza (company_id, especie, nombre);

CREATE INDEX IF NOT EXISTS idx_raza_especie_nombre_activa
    ON raza (especie, nombre)
    WHERE activo = true;

-- Roles, vistas y permisos
CREATE INDEX IF NOT EXISTS idx_roles_company_name
    ON roles (company_id, name);

CREATE INDEX IF NOT EXISTS idx_usuario_por_rol_usuario
    ON usuario_por_rol (usuario_id);

CREATE INDEX IF NOT EXISTS idx_usuario_por_rol_rol
    ON usuario_por_rol (rol_id);

CREATE INDEX IF NOT EXISTS idx_rol_vista_permisos_rol
    ON rol_vista_permisos (rol_id);

CREATE INDEX IF NOT EXISTS idx_usuario_por_rol_permisos_upr
    ON usuario_por_rol_permisos (usuario_por_rol_id);

-- Busquedas textuales con LIKE '%texto%' y coincidencia aproximada.
CREATE INDEX IF NOT EXISTS idx_mascota_nombre_trgm
    ON mascota USING gin (lower(nombre_completo) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_usuario_nombre_completo_trgm
    ON usuario USING gin (lower(nombre || ' ' || apellido) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_usuario_email_trgm
    ON usuario USING gin (lower(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_prescripciones_medicamento_trgm
    ON prescripciones USING gin (lower(medicamento) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_prescripciones_principio_activo_trgm
    ON prescripciones USING gin (lower(principio_activo) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_prescripciones_created_at
    ON prescripciones (created_at DESC);
