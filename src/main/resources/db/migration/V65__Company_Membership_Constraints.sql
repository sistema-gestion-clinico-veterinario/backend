-- Fase 2.5 del plan de separacion identidad/empresa: constraints e indices.
-- Separada deliberadamente de V63/V64 (estructura/backfill) para que, si algo
-- fallara en el backfill, nunca se llegue a modificar restricciones.
-- Ver C:\Users\Usuario\.claude\plans\hidden-purring-abelson.md para el plan completo.

-- 1. numero_colegiatura: se libera tras una baja (reingreso = fila nueva).
ALTER TABLE empleado DROP CONSTRAINT ukq31dudn3vwixpsh4de9spphds;
CREATE UNIQUE INDEX uq_empleado_colegiatura_activo
    ON empleado (numero_colegiatura, company_id)
    WHERE estado = true AND numero_colegiatura IS NOT NULL;

-- 2. numero_documento: se mantiene reservado dentro de la misma empresa
-- incluso inactivo (trazabilidad de facturacion/historial). Por esto el
-- reingreso de un Apoderado a la MISMA empresa reactiva la fila existente
-- en vez de insertar una nueva (ver plan, seccion "Decision de modelo de datos").
ALTER TABLE apoderado DROP CONSTRAINT uk7k1pgodhimc8s6yegl3tnhltm;
CREATE UNIQUE INDEX uq_apoderado_documento_empresa
    ON apoderado (numero_documento, company_id);

-- 3. Elimina el UNIQUE(user_id) heredado del @OneToOne actual en ambas
-- tablas (bloquearia por completo el objetivo de esta migracion: permitir
-- varias filas Empleado/Apoderado por Usuario). Para Empleado se reemplaza
-- por el indice parcial de abajo (a lo sumo una fila ACTIVA por usuario,
-- cierra la condicion de carrera del registro simultaneo en dos empresas,
-- ver plan "Riesgo de concurrencia" en Fase 4). Para Apoderado NO se
-- reemplaza con nada: puede haber varias filas activas a la vez a proposito
-- (cliente de varias empresas simultaneamente).
ALTER TABLE empleado DROP CONSTRAINT ukrt96qoac8tt9xjkxdb065hcyl;
ALTER TABLE apoderado DROP CONSTRAINT ukhwhqtw8dsjxa0t3l23087igp7;

-- Verificado 0 filas con estado IS NULL en dev antes de crear este indice;
-- verificar lo mismo en produccion antes de aplicar esta migracion ahi.
CREATE UNIQUE INDEX uq_empleado_activo_por_usuario
    ON empleado (user_id)
    WHERE estado = true;

-- 4. usuario_por_rol: dos indices parciales en vez de un UNIQUE simple,
-- porque Postgres no considera iguales dos valores NULL de company_id
-- (un UNIQUE(usuario_id, rol_id, company_id) no protegeria los roles
-- globales). Verificado con datos reales: 4 roles globales, 5 asignaciones,
-- 0 duplicados actuales.
CREATE UNIQUE INDEX uq_usuario_por_rol_global
    ON usuario_por_rol (usuario_id, rol_id)
    WHERE company_id IS NULL;

CREATE UNIQUE INDEX uq_usuario_por_rol_empresa
    ON usuario_por_rol (usuario_id, rol_id, company_id)
    WHERE company_id IS NOT NULL;

COMMENT ON INDEX uq_apoderado_documento_empresa IS 'A diferencia de numero_colegiatura, no se libera tras una baja: se mantiene reservado dentro de la misma empresa por trazabilidad. Ver plan, excepcion de reingreso para Apoderado.';
COMMENT ON INDEX uq_empleado_activo_por_usuario IS 'Un usuario no puede tener mas de una relacion laboral activa a la vez, en ninguna empresa. Protege contra condicion de carrera; la regla de negocio con mensaje amigable vive en CompanyMembershipService.';
