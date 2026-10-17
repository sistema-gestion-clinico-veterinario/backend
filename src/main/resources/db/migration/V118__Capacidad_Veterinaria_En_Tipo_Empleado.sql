-- La capacidad clínica debe almacenarse explícitamente y no inferirse del
-- nombre del cargo. Para conservar el comportamiento histórico, los tipos que
-- ya permitían especialidades se inicializan como veterinarios.
ALTER TABLE tipo_empleado
    ADD COLUMN IF NOT EXISTS es_veterinario BOOLEAN;

UPDATE tipo_empleado
   SET es_veterinario = COALESCE(permite_especialidades, FALSE)
 WHERE es_veterinario IS NULL;

ALTER TABLE tipo_empleado
    ALTER COLUMN es_veterinario SET DEFAULT FALSE,
    ALTER COLUMN es_veterinario SET NOT NULL;

COMMENT ON COLUMN tipo_empleado.es_veterinario IS
    'Indica si el tipo de empleado puede ejercer funciones clínicas veterinarias';
y 