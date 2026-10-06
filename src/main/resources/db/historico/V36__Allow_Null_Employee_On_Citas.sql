-- V36: Permitir que las citas-cobro internas de la cartilla queden sin empleado.
-- Un SUPER_ADMIN/ADMIN es un usuario del sistema que puede no tener fila en
-- 'empleados'; al registrar una aplicacion preventiva se crea una cita-cobro
-- interna y hasta ahora 'citas.empleado_id' era NOT NULL -> el registro fallaba.
--
-- NOTA: ddl-auto=update NO relaja constraints existentes. Ejecuta este script
-- manualmente (como V35). Es idempotente.

DO $$
BEGIN
    -- Permitir empleado_id nulo en citas (superadmin/admin sin empleado propio)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='citas' AND column_name='empleado_id'
          AND is_nullable='NO'
    ) THEN
        ALTER TABLE citas ALTER COLUMN empleado_id DROP NOT NULL;
    END IF;
END $$;