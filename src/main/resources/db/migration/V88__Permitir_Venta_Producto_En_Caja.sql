-- El concepto VENTA_PRODUCTO fue añadido al dominio cuando Caja comenzó a
-- registrar las ventas sin cita. Las bases existentes pueden conservar el
-- CHECK generado antes de ese cambio y rechazar el nuevo valor.

DO $$
DECLARE
    concepto_type_name TEXT;
    concepto_type_kind "char";
    restriccion RECORD;
BEGIN
    SELECT tipo.typname, tipo.typtype
      INTO concepto_type_name, concepto_type_kind
      FROM pg_attribute atributo
      JOIN pg_class tabla ON tabla.oid = atributo.attrelid
      JOIN pg_namespace esquema ON esquema.oid = tabla.relnamespace
      JOIN pg_type tipo ON tipo.oid = atributo.atttypid
     WHERE esquema.nspname = current_schema()
       AND tabla.relname = 'movimiento_caja'
       AND atributo.attname = 'concepto'
       AND NOT atributo.attisdropped;

    IF concepto_type_kind = 'e' THEN
        EXECUTE format('ALTER TYPE %I ADD VALUE IF NOT EXISTS %L', concepto_type_name, 'VENTA_PRODUCTO');
    END IF;

    FOR restriccion IN
        SELECT constraint_data.conname
          FROM pg_constraint constraint_data
          JOIN pg_class tabla ON tabla.oid = constraint_data.conrelid
          JOIN pg_namespace esquema ON esquema.oid = tabla.relnamespace
         WHERE esquema.nspname = current_schema()
           AND tabla.relname = 'movimiento_caja'
           AND constraint_data.contype = 'c'
           AND pg_get_constraintdef(constraint_data.oid) ILIKE '%concepto%'
    LOOP
        EXECUTE format('ALTER TABLE movimiento_caja DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END $$;

ALTER TABLE movimiento_caja
    ADD CONSTRAINT chk_movimiento_caja_concepto
    CHECK (concepto::text IN (
        'PAGO_CITA',
        'VENTA_PRODUCTO',
        'CANCELACION_DEVOLUCION',
        'GASTO_OPERATIVO',
        'OTRO'
    ));
