# Migraciones de la base de datos

Esta carpeta contiene solo las migraciones **nuevas**, de la **V104** en adelante. Flyway las ejecuta en orden y
guarda en la tabla `flyway_schema_history` cuáles ya se aplicaron.

Las migraciones V1 a V103 se aplicaron a mano en producción antes de adoptar Flyway; viven en `../historico` y
Flyway no las lee. La línea base es la versión 103.

## Cómo agregar una migración
1. Crea `V104__Descripcion_Corta.sql` con el siguiente número (nunca repitas una versión).
2. Si creas una tabla en el esquema `public`, activa RLS en la misma migración:
   `ALTER TABLE nombre ENABLE ROW LEVEL SECURITY;`
3. Una migración ya aplicada no se edita: un cambio es otra migración con el número siguiente.
4. Súbela a GitHub junto con el código que la necesita. Se aplica sola al arrancar el backend si
   `FLYWAY_ENABLED=true` en el entorno.

## Activarlo en un entorno
- `FLYWAY_ENABLED=true`.
- Con el esquema ya existente y sin `flyway_schema_history`, Flyway lo toma como versión 103 y aplica solo lo posterior.
- Una base vacía no se crea con estas migraciones: hay que crearla desde un volcado del esquema o con
  `JPA_DDL_AUTO=update` y luego activar Flyway.
- Para que Flyway use otra conexión distinta a la de la aplicación: `SPRING_FLYWAY_URL`, `SPRING_FLYWAY_USER`
  y `SPRING_FLYWAY_PASSWORD`. Debe ser la conexión directa o la de sesión (puerto 5432), no el pooler de transacciones.
- En Supabase, la primera vez que Flyway corre crea la tabla `flyway_schema_history` en `public`. Actívale RLS
  después: `ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;` (Flyway sigue funcionando con `postgres`).
