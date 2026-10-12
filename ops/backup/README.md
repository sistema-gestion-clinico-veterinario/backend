# Respaldos de SoftVet

Copia cifrada y diaria de la base de datos (Supabase) y semanal de los archivos (Supabase Storage), guardada fuera de
Supabase. Implementa el diseño de `PDP-03 §3` y sirve de evidencia para RNF-11.

## Cómo funciona

```
Supabase (PostgreSQL) ──pg_dump──> volcado ──restaura en una base temporal y compara filas──> cifra con GPG ──> bucket externo
                                                                                               (solo sale el .gpg)
```

1. `pg_dump` del esquema `public` en formato comprimido.
2. Cuenta las filas de cada tabla antes y después del volcado.
3. **Restaura el volcado en una base temporal** (en el flujo de GitHub es un contenedor PostgreSQL desechable) y exige que
   las filas coincidan. Un respaldo que no se puede restaurar hace fallar el trabajo.
4. Cifra con la **clave pública** (GPG, AES-256). El volcado sin cifrar se borra y nunca sale de la máquina que lo generó.
5. Sube el `.gpg` y un manifiesto (`fecha`, `sha256`, `tamaño`, filas por tabla, resultado de la verificación) y
   comprueba que el tamaño remoto coincide.
6. Avisa a un monitor externo (Healthchecks.io); si un día no llega el aviso, te escribe.

Quién puede qué:

| Pieza | Está en | Permite |
|---|---|---|
| Clave **pública** GPG | Secreto de GitHub | Solo cifrar. Si se filtra, no revela nada. |
| Clave **privada** GPG | Solo tú, fuera de línea | Descifrar y restaurar. **Sin ella los respaldos no sirven.** |
| Usuario de BD de solo lectura | Secreto de GitHub | Leer la base; no puede modificarla. |
| Llave del bucket | Secreto de GitHub | Escribir en un solo bucket. |

## Puesta en marcha (una sola vez)

### 1. Bucket externo (recomendado: Backblaze B2)
- Crea un bucket **privado**. Región cercana a tus datos.
- Crea una *Application Key* limitada a ese bucket (lectura y escritura). Guarda `keyID`, `applicationKey` y el *endpoint*
  S3 (`https://s3.<región>.backblazeb2.com`).
- Aplica la retención (7 diarios, 4 semanales, 6 mensuales):
  ```bash
  aws s3api put-bucket-lifecycle-configuration --bucket TU_BUCKET \
    --endpoint-url https://s3.REGION.backblazeb2.com \
    --lifecycle-configuration file://ops/backup/retencion.json
  ```
  Si tu proveedor no admite esa orden, configura las mismas reglas en su panel (borrar tras 8, 31 y 190 días según la carpeta).
- Cloudflare R2 también funciona con los mismos scripts.

### 1b. Alternativa gratuita: Google Drive (en lugar del bucket)
Funciona con `rclone`. Es menos estable que un bucket: la autorización puede invalidarse (cambio de contraseña de Google,
revocación de acceso) y entonces los respaldos fallan; el monitor de Healthchecks te avisa.

- Usa una cuenta de Gmail **solo para respaldos**, con verificación en dos pasos y una contraseña única. No uses tu cuenta personal.
  Tu PDP-03 recomienda cuenta corporativa; con Gmail personal documenta la excepción.
- Instala rclone en tu equipo: `winget install Rclone.Rclone`.
- Genera la autorización limitada a los archivos que crea el propio respaldo (alcance `drive.file`):
  ```bash
  rclone authorize "drive" "eyJzY29wZSI6ImRyaXZlLmZpbGUifQ=="
  ```
  Se abre el navegador, inicias sesión con el Gmail de respaldos y aceptas. rclone imprime un JSON entre
  `--->` y `<---`: ese JSON es el secreto `RCLONE_DRIVE_TOKEN`.
- En GitHub crea la **variable** (no secreto) `BACKUP_DESTINO` con el valor `drive`. Los secretos `BACKUP_S3_*` no hacen falta.
- Las copias se guardan en la carpeta `softvet-respaldos` de ese Drive. Como Drive no tiene reglas de retención, el propio script
  borra de forma permanente las copias vencidas (8, 31 y 190 días según la carpeta).
- Para restaurar, descarga el `.gpg` y su manifiesto desde Drive y usa `restaurar.sh` igual que arriba.

### 2. Claves de cifrado (en un equipo de confianza)
```bash
gpg --quick-generate-key "SoftVet Respaldos <correo-del-responsable>" default default never
gpg --armor --export "SoftVet Respaldos" > publica.asc            # esta va al secreto de GitHub
gpg --armor --export-secret-keys "SoftVet Respaldos" > privada.asc   # esta NO sube a ningún lado
```
- Protege la clave privada con una **frase de paso** larga cuando GPG la pida.
- Guarda `privada.asc` y la frase en **dos lugares distintos** (gestor de contraseñas y una copia impresa o en USB en otro sitio).
- Pierde la clave privada y pierdes todos los respaldos. No la guardes en el mismo bucket ni en el repositorio.

### 3. Usuario de solo lectura en Supabase
En el editor SQL de Supabase:
```sql
CREATE ROLE respaldo LOGIN PASSWORD 'UNA-CLAVE-LARGA-Y-UNICA';
GRANT pg_read_all_data TO respaldo;
```
La URL de conexión debe ser la del **Session pooler** (Settings → Database → Connection string → Session pooler), porque
los ejecutores de GitHub solo tienen IPv4 y la conexión directa de Supabase es IPv6. Queda así:
`postgresql://respaldo.REFERENCIA:CLAVE@aws-0-REGION.pooler.supabase.com:5432/postgres?sslmode=require`

### 4. Secretos en GitHub (Settings → Secrets and variables → Actions)
| Secreto | Valor |
|---|---|
| `BACKUP_DB_URL` | URL del paso 3 |
| `BACKUP_GPG_PUBLIC_KEY` | contenido de `publica.asc` |
| `BACKUP_S3_BUCKET`, `BACKUP_S3_ENDPOINT`, `BACKUP_S3_REGION` | datos del bucket (no hacen falta si usas Drive) |
| `BACKUP_S3_KEY_ID`, `BACKUP_S3_APPLICATION_KEY` | llave del bucket (no hacen falta si usas Drive) |
| `RCLONE_DRIVE_TOKEN` | solo con Drive: el JSON del paso 1b |
| `HEALTHCHECK_BASE_URL` | URL de un chequeo de Healthchecks.io (sin barra final); periodo 1 día, gracia 3 horas |
| `SUPABASE_S3_ENDPOINT`, `SUPABASE_S3_ACCESS_KEY`, `SUPABASE_S3_SECRET_KEY`, `SUPABASE_S3_REGION`, `SUPABASE_BUCKET`, `HEALTHCHECK_ARCHIVOS_URL` | solo si quieres respaldar los archivos (Supabase → Storage → S3 Connection) |

### 5. Primera ejecución
En GitHub: Actions → *Respaldo* → *Run workflow*. Revisa que aparezca el archivo en el bucket y que el chequeo se ponga en verde.

> **El repositorio `backend` es público.** El trabajo no imprime URLs ni claves y GitHub oculta los secretos en los registros,
> pero no agregues `set -x` ni subas el volcado como *artifact*. Para más aislamiento, ejecuta este flujo desde un repositorio
> privado aparte. Además, en repositorios públicos GitHub **desactiva los flujos programados tras 60 días sin actividad**;
> el aviso de Healthchecks existe para enterarte si pasa.

## Restaurar

Hazlo en una base **nueva o de pruebas**; la restauración borra el esquema `public` del destino.

```bash
# 1. Descarga el .gpg y su manifiesto desde el bucket
aws s3 cp s3://BUCKET/base/diario/AAAA-MM-DD/softvet-base-....dump.gpg . --endpoint-url ENDPOINT
aws s3 cp s3://BUCKET/base/diario/AAAA-MM-DD/softvet-base-....manifest.json . --endpoint-url ENDPOINT

# 2. Importa tu clave privada (una vez por equipo)
gpg --import privada.asc

# 3. Restaura
bash ops/backup/restaurar.sh --archivo softvet-base-....dump.gpg --destino "postgresql://usuario:clave@host:5432/base_nueva" --confirmo
```
El script comprueba la suma de verificación, descifra, restaura y compara las filas con el manifiesto.

**Ensayo trimestral (exigido por PDP-03 y RNF-11):** restaura el respaldo más reciente en una base vacía, abre la aplicación
contra ella y anota fecha, quién lo hizo, el respaldo usado y el resultado. Ese registro es la evidencia.

## Probar los scripts sin tocar producción
```bash
bash ops/backup/prueba-local.sh
```
Levanta un PostgreSQL temporal, respalda, cifra, restaura y comprueba datos, acentos, binarios y relaciones; también verifica
que un archivo dañado, la falta de `--confirmo`, la falta de variables y una clave equivocada **fallan**. Necesita
`pg_dump`, `pg_restore`, `psql`, `gpg` y `jq`.

## Lo que este respaldo no cubre
- **Las variables de entorno de Render y Vercel** no están en el repositorio; anótalas en tu gestor de contraseñas.
- **Las migraciones SQL** ya están en el repositorio (`src/main/resources/db`).
- **El respaldo de archivos** (`respaldar-archivos.sh`) no se ha probado contra Supabase Storage real. Descarga todo el bucket en
  cada ejecución: si crece mucho, hay que pasar a copias incrementales.
- **Esquemas distintos de `public`** (Auth, Storage) no se incluyen; la aplicación solo usa `public`.

## Datos personales
Los respaldos contienen datos de salud de clientes. Están cifrados, pero también cuentan para la retención: una solicitud de
cancelación (RF-SEG-01) no se refleja en las copias existentes hasta que vencen (máximo 6 meses). Documéntalo en el aviso de
privacidad y limita quién tiene la clave privada.
