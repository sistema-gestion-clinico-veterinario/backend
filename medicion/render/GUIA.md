# Medición real en Render: guía paso a paso

Se miden dos servicios gratuitos de prueba, con datos sintéticos. No se toca producción.

| Servicio | Código | Base de datos |
|---|---|---|
| `medicion-antes` | rama `main` (lo que corre hoy) | proyecto de Supabase `medicion-antes` |
| `medicion-despues` | rama `medicion/despues` (`main` + cambios green) | proyecto de Supabase `medicion-despues` |

Todo lo que se escribe aquí lo haces tú; nunca pegues contraseñas en el chat.

## 1. Claves y contraseña de las cuentas de medición (en tu computadora, unos segundos)

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl rsa -pubout -in private_key.pem -out public_key.pem
```

Genera un par nuevo; no uses las claves de prueba del repositorio (son públicas). Inventa también una contraseña para las cuentas de medición: solo letras y números, de 12 o más caracteres (será `MEDICION_CLAVE`). Borra los dos `.pem` al terminar la medición.

## 2. Dos proyectos en Supabase (plan gratuito)

1. Crea `medicion-antes` y `medicion-despues`, cada uno con su contraseña de base de datos (solo letras y números).
2. En cada proyecto, botón **Connect** → **Session pooler** (puerto 5432, compatible con IPv4). Anota:
   - el servidor (`aws-0-<región>.pooler.supabase.com`),
   - el usuario (`postgres.<referencia-del-proyecto>`).
3. Esas dos cadenas serán los secretos `MEDICION_DB_ANTES` y `MEDICION_DB_DESPUES` (paso 4), con este formato:
   `postgresql://postgres.<referencia>:<contraseña>@aws-0-<región>.pooler.supabase.com:5432/postgres`

No uses el *Transaction pooler* (puerto 6543): rompe las sentencias preparadas del backend.

## 3. Dos servicios en Render (plan gratuito)

Para cada uno: **New → Web Service → Public Git Repository**, URL `https://github.com/sistema-gestion-clinico-veterinario/backend`.

- **Name:** `medicion-antes` / `medicion-despues`
- **Branch:** `main` / `medicion/despues`
- **Language:** Docker (`Dockerfile`, ruta por defecto)
- **Region:** la misma que la de Supabase, si se puede
- **Instance type:** Free
- **Auto-Deploy: No** (si `main` avanza, el servicio ANTES debe seguir en el mismo código)
- **Health Check Path:** `/api/v1/actuator/health`

### Variables de entorno (las mismas en los dos; cambian solo las de base de datos)

| Variable | Valor |
|---|---|
| `PORT` | `8080` |
| `EJECUTAR_PRUEBAS` | `false` |
| `DATABASE_URL` | `jdbc:postgresql://aws-0-<región>.pooler.supabase.com:5432/postgres` |
| `DB_USERNAME` | `postgres.<referencia-del-proyecto>` |
| `DB_PASSWORD` | la contraseña de la base de ese proyecto |
| `DB_POOL_MAX_SIZE` | `5` |
| `DB_POOL_MIN_IDLE` | `1` |
| `JPA_DDL_AUTO` | `update` |
| `FLYWAY_ENABLED` | `false` |
| `JWT_PRIVATE_KEY` | `file:/etc/secrets/private_key.pem` |
| `JWT_PUBLIC_KEY` | `file:/etc/secrets/public_key.pem` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost` |
| `APP_URL` | `http://localhost` |
| `TRUSTED_PROXY_COUNT` | `1` |
| `RATE_LIMIT_LOGIN_PER_MINUTE` | `1000` |
| `RATE_LIMIT_LOGIN_PER_ACCOUNT_PER_15_MINUTES` | `1000` |
| `SWAGGER_ENABLED` | `false` |
| `REMINDERS_CRON` | `0 0 0 1 1 *` |
| `MAIL_HOST` | `127.0.0.1` |
| `MAIL_PORT` | `2525` |
| `MAIL_USERNAME` | `desactivado` |
| `MAIL_PASSWORD` | `desactivado` |
| `MAIL_FROM` | `medicion@softvet.local` |
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | `false` |
| `SUPABASE_URL` | `http://supabase-desactivado.local` |
| `SUPABASE_SERVICE_ROLE_KEY` | `desactivado` |
| `SUPABASE_BUCKET` | `desactivado` |
| `IA_RADIOGRAFIA_URL` | `http://ia-desactivada.local` |
| `IA_LABORATORIO_URL` | `http://ia-desactivada.local` |
| `IA_DIAGNOSTICO_URL` | `http://ia-desactivada.local` |

No pongas `JAVA_OPTS`: así corre con las banderas de producción del `Dockerfile` (`-Xmx192m`, etc.).

### Secret Files

En **Environment → Secret Files** crea `private_key.pem` y `public_key.pem` con el contenido de los archivos del paso 1 (Render los monta en `/etc/secrets/`). Las mismas claves sirven para los dos servicios.

El primer despliegue puede tardar mucho (compilar con poca CPU y arrancar con una fracción de CPU). Espera a que el panel diga **Live**. Si falla, copia las últimas líneas del registro (sin contraseñas) y me las pasas.

`JPA_DDL_AUTO=update` crea las tablas en la base vacía y nunca las borra. No lo cambies a `create`: en cada reinicio borraría los datos.

## 4. Secretos en GitHub

Repositorio `backend` → **Settings → Secrets and variables → Actions → New repository secret**:

| Secreto | Valor |
|---|---|
| `MEDICION_URL_ANTES` | URL pública de `medicion-antes`, por ejemplo `https://medicion-antes.onrender.com` |
| `MEDICION_URL_DESPUES` | URL pública de `medicion-despues` |
| `MEDICION_DB_ANTES` | la cadena `postgresql://…` del proyecto `medicion-antes` |
| `MEDICION_DB_DESPUES` | la cadena `postgresql://…` del proyecto `medicion-despues` |
| `MEDICION_CLAVE` | la contraseña inventada en el paso 1 |

## 5. Fases (cada una es un push a la rama `medicion/render`)

El archivo `medicion/render/FASE` decide qué hace el flujo. Empieza en `verificar`.

```bash
git switch medicion/render
git push origin medicion/render
```

1. **`verificar`:** espera a que cada servicio responda (arranque en frío), lee la base, comprueba si hay datos y mide el límite de peticiones por IP. Aún no escribe nada.
2. **`sembrar`:** carga 6 000 citas en la clínica grande (más 5 clínicas pequeñas), crea las 3 cuentas de medición y aplica las migraciones de cada versión.
3. **`medir`:** calentamiento y 4 rondas alternando ANTES y DESPUÉS.

Para pasar de fase:

```bash
echo sembrar > medicion/render/FASE
git commit -am "Medición en Render: fase sembrar"
git push origin medicion/render
```

Después se repite con `medir`. Los resultados se publican en la rama `medicion/resultados-render`; las direcciones y cadenas de conexión salen sustituidas por marcas.

## 6. CPU y memoria

GitHub no ve el consumo del servidor. Durante la fase `medir`:

1. Abre cada servicio en Render → pestaña **Metrics**.
2. Anota las horas de inicio y fin de la fase (están en `logs/orquestador.log`) y haz una captura de los gráficos de CPU y memoria de ese intervalo.
3. Pásame las capturas (o los valores máximo y medio). Si el plan gratuito no muestra esas gráficas, lo dejamos documentado y se mide solo la latencia.

## 7. Al terminar

Borra los dos servicios de Render, los dos proyectos de Supabase, los cinco secretos de GitHub y los `.pem`.

## Límites

- Las latencias incluyen la red entre GitHub y Render y los arranques en frío.
- Un servicio gratuito se apaga a los 15 minutos sin tráfico; la fase `medir` mantiene tráfico continuo.
- Los números dependen del momento del día y del vecino de máquina en Render: se repiten rondas y se publica la mediana con el rango.
- Los datos son sintéticos (6 000 citas en la clínica grande, no 20 000, para no agotar la memoria de 192 MB de heap).

## 8. Ver k6 en vivo en Grafana Cloud (fase `vivo`)

1. En Grafana Cloud, en tu stack: **Testing & synthetics → Performance → Settings → Access → Personal token**. Crea el token y cópialo. Ahí mismo está el **Stack ID**.
2. Guarda tres secretos nuevos en GitHub: `MEDICION_K6_TOKEN` (el token), `MEDICION_K6_STACK` (el Stack ID) y `MEDICION_K6_PROJECT` (el número del proyecto: en Performance Testing abre «Default project» y copia el número del final de la dirección).
3. Con `FASE` en `vivo`, un push a `medicion/render` envía una ronda de ANTES y otra de DESPUÉS a Grafana Cloud mientras corren. Se ven en **Testing & synthetics → Performance → Tests and test runs**.
4. Al terminar, revoca el token en la misma pantalla.

## 9. Prueba de carga escalonada (fase `carga`)

Con `FASE` en `carga`, un push a `medicion/render` lanza **las dos versiones a la vez** desde GitHub, con 1, 10, 25, 50 y 100 usuarios virtuales (3 minutos en cada nivel, 3 s de pausa entre secciones del recorrido) y las envía a Grafana Cloud en vivo como «Render carga escalonada - antes» y «Render carga escalonada - despues». Dura unos 20 minutos más la espera de arranque.

- **Antes de subir:** en Grafana, **Testing & synthetics → Performance → Settings → Stack limits**, mira «Max VUs per test» de tu plan. Si es menor que 100, baja el último nivel (variable `NIVELES_CARGA` en `medicion/render/ejecutar.sh`).
- **Cuota:** cada usuario virtual activo en una hora cuenta como 1 VUh: 100 usuarios son unas 100 VUh por versión, unas 220 en total de las 500 del mes.
- **Qué mide:** en qué nivel cada versión empieza a dar errores o a pasarse de tiempo. Con 0,15 CPU y 192 MB de heap, el servicio puede saturarse o reiniciarse; son servicios de prueba.
- **Límite de interpretación:** 100 usuarios virtuales sin pausas largas equivalen a mucho más que 100 personas reales: no es una prueba de «100 usuarios reales».
