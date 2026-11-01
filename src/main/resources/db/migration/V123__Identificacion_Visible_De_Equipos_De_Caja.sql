ALTER TABLE caja
    ADD COLUMN IF NOT EXISTS dispositivo_nombre VARCHAR(80);

UPDATE caja
SET dispositivo_nombre = 'Equipo de ' || nombre
WHERE dispositivo_token_hash IS NOT NULL
  AND (dispositivo_nombre IS NULL OR BTRIM(dispositivo_nombre) = '');

COMMENT ON COLUMN caja.dispositivo_nombre IS
    'Nombre visible asignado por la clínica al equipo físico vinculado al punto de cobro.';
