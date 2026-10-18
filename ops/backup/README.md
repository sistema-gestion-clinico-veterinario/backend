# Respaldos de SoftVet

El workflow `.github/workflows/backup.yml` genera un punto de recuperación cifrado y verificado. La base de datos y los archivos de Supabase Storage se publican en el destino remoto bajo el mismo identificador.

## Acceso temporal a PostgreSQL

La base no debe permanecer expuesta mediante `0.0.0.0/0`. Antes del volcado, el workflow:

1. obtiene la IPv4 pública del runner;
2. aplica la lista permanente más esa dirección `/32`;
3. ejecuta y verifica el respaldo;
4. restaura la lista permanente mediante un trabajo independiente con `always()`.

Al comenzar una ejecución se reemplaza cualquier autorización temporal anterior. El grupo de concurrencia `respaldo` evita que dos ejecuciones modifiquen la lista simultáneamente.

La separación del cierre permite ejecutarlo aunque el trabajo de base de datos falle o agote su tiempo. La integración usa los endpoints experimentales de restricciones de red de la API de administración de Supabase. Un cambio incompatible del proveedor hará fallar el workflow de forma cerrada: el volcado no continuará si no se confirmó la autorización.

## Configuración de GitHub

Crear el secreto de Actions:

| Nombre | Contenido |
| --- | --- |
| `SUPABASE_MANAGEMENT_ACCESS_TOKEN` | Token de administración de alcance fino, limitado al proyecto, con permisos `database_network_restrictions_read` y `database_network_restrictions_write`. |

Crear las variables de Actions:

| Nombre | Ejemplo | Uso |
| --- | --- | --- |
| `SUPABASE_PROJECT_REF` | `abcdefghijklmnopqrst` | Referencia del proyecto de Supabase. |
| `SUPABASE_DB_ALLOWED_CIDRS_BASE` | `["203.0.113.10/32"]` | Lista JSON de IPv4 que deben permanecer autorizadas. |
| `SUPABASE_DB_ALLOWED_CIDRS_V6_BASE` | `[]` | Lista JSON de IPv6 permanentes. |

La configuración base debe contener al menos un CIDR entre ambas listas. El script rechaza listas inválidas y reglas `/0`. Deben copiarse todas las reglas permanentes existentes, incluidas las necesarias para los servicios que conectan directamente a PostgreSQL.

## Operación

Después de cambiar las reglas permanentes en Supabase, se deben actualizar también las variables base de GitHub. Si ambas configuraciones divergen, la siguiente ejecución restaurará deliberadamente la configuración declarada en GitHub.

Tras cada ejecución se debe comprobar que:

- `Preparar el punto de recuperación`, `Respaldar la base de datos` y `Respaldar los archivos` terminaron correctamente;
- el paso final confirmó `Restricciones permanentes restauradas`;
- no quedó la IP temporal del runner en Supabase;
- el destino contiene los componentes cifrados del mismo punto de recuperación.

## Monitoreo en SoftVet

El workflow puede publicar un resumen operativo sanitizado en el panel **Respaldo y recuperación**. Esta integración no transmite archivos, credenciales, hashes, nombres de buckets ni claves GPG.

Configurar en GitHub Actions:

| Tipo | Nombre | Contenido |
| --- | --- | --- |
| Variable | `BACKUP_REPORT_URL` | URL base del backend con su contexto, por ejemplo `https://backend.example.com/api/v1`. |
| Secreto | `BACKUP_REPORT_TOKEN` | Valor aleatorio de 32 bytes o más, exclusivo para reportar respaldos. |

Configurar exactamente el mismo secreto en el entorno del backend:

| Nombre | Contenido |
| --- | --- |
| `BACKUP_REPORT_TOKEN` | El mismo valor aleatorio guardado en GitHub Actions. |

El endpoint de reporte solo acepta HTTPS desde el workflow, compara el token en tiempo constante y registra cada combinación de `run_id` y `run_attempt` una sola vez. Una ejecución finalizada es inmutable. El token de reporte no concede acceso para consultar datos, descargar respaldos ni ejecutar restauraciones.

## Recuperación verificada

`restaurar.sh` exige un manifiesto firmado, valida el SHA-256 antes de modificar el destino y restaura PostgreSQL por etapas. Primero recupera `pre-data`, luego prepara las extensiones declaradas en `RESTORE_EXTENSIONS` (por defecto `pg_trgm`), después carga `data` y finalmente `post-data`. Cada etapa falla de forma cerrada y dentro de su propia transacción. Al terminar compara exactamente el conjunto de tablas y la cantidad de filas con el manifiesto.

`restaurar-archivos.sh` solo acepta paquetes TAR con directorios y archivos regulares, rechaza rutas absolutas, recorridos `..`, enlaces y entradas duplicadas. Antes de escribir valida que todos los buckets de recuperación existan y estén vacíos. Después de cada sincronización vuelve a descargar el bucket y compara rutas, tamaños y SHA-256 archivo por archivo. Una diferencia impide declarar la recuperación como satisfactoria.

Las restauraciones deben realizarse siempre sobre destinos aislados. Los scripts no crean buckets ni aceptan sobrescribir contenido existente.
