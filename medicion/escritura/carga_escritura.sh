#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"
SAL="$RAIZ/resultados"
mkdir -p "$SAL/logs" "$SAL/niveles"

ANTES_DIR="${ANTES_DIR:-$RAIZ/.antes}"
DESPUES_DIR="${DESPUES_DIR:-$RAIZ/.despues}"
CONFIG="$(tr -d '[:space:]' < "$RAIZ/medicion/capacidad/CONFIG")"
CPUS="${CPUS:-1}"
MEMORIA="${MEMORIA:-2g}"
CUENTAS="${CUENTAS:-1}"
PLAN_CARGA="$RAIZ/medicion/escritura/PLAN_CARGA"
ITER_POR_VU="${ITER_POR_VU:-19}"
PAUSA_S="${PAUSA_S:-15}"
PESOS="${PESOS:-30,35,30,5}"
TIMEOUT_HTTP="${TIMEOUT_HTTP:-30s}"
POOL="${POOL:-10}"
PGPASS="medicion"
PUERTO=18080
SLUG="clinica-medicion"
USUARIO="medicion"
CLAVE="$(openssl rand -hex 12)"

BASE_JVM="-XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m"
OBSERVACION_JVM="-Xlog:gc,gc+metaspace:file=/tmp/jvm_gc.log:uptime,tags -Xlog:class+load:file=/tmp/jvm_clases.log:uptime,tags -Dspring.mail.properties.mail.smtp.auth=false -Dspring.mail.properties.mail.smtp.starttls.enable=false -Dspring.mail.properties.mail.smtp.starttls.required=false"
opciones_jvm() {
  case "$1" in
    A) JAVA_OPTS_LAB="-Xmx192m $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
    B) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
    C) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m" ;;
    D) JAVA_OPTS_LAB="-Xmx1g -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    E) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    *) echo "configuración desconocida: '$1' (usa A, B, C, D o E)"; exit 1 ;;
  esac
}
opciones_jvm "$CONFIG"

NUCLEOS="$(nproc)"
if [ "$NUCLEOS" -ge 4 ]; then
  PIN_PG="--cpuset-cpus 2"
  PIN_BACK="--cpuset-cpus 1"
  PIN_K6="taskset -c 0,3"
else
  PIN_PG=""
  PIN_BACK=""
  PIN_K6=""
fi

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$SAL/logs/orquestador.log"; }
fallo() { log "FALLO: $*"; exit 1; }
dir_de() { if [ "$1" = "antes" ]; then echo "$ANTES_DIR"; else echo "$DESPUES_DIR"; fi; }
psql_db() { local db="$1"; shift; docker exec -i sv-pg psql -U postgres -d "$db" -v ON_ERROR_STOP=1 -q "$@"; }

registrar_entorno() {
  {
    echo "fecha: $(date -u +%FT%TZ)"
    echo "núcleos del equipo: $NUCLEOS"
    free -m | sed -n 2p | awk '{print "memoria total del equipo (MB): "$2}'
    echo "recursos del backend: $CPUS CPU, $MEMORIA de RAM (como Render Standard)"
    echo "configuraciones de la JVM:"
    for c in C D E; do opciones_jvm "$c"; echo "  $c -> $JAVA_OPTS_LAB"; done
    echo "usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control"
    echo "correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)"
    opciones_jvm D
    echo "pool de conexiones: $POOL"
    echo "pausa entre acciones: $PAUSA_S s (con variación aleatoria)"
    echo "mezcla de acciones (panel, agenda, historia, reportes, recepción, atención, cobro, cancelar): 22,18,15,2,13,12,10,8 %"
    echo "cuentas de medición: una por usuario virtual, $TOTAL_CUENTAS en total"
    echo "docker: $(docker version --format '{{.Server.Version}}')"
    echo "k6: $(k6 version | head -1)"
    echo "antes:   $(git -C "$ANTES_DIR" rev-parse HEAD)"
    echo "despues: $(git -C "$DESPUES_DIR" rev-parse HEAD)"
    echo "fijado de CPU: PG='$PIN_PG' backend='$PIN_BACK' k6='$PIN_K6'"
    echo "plan de niveles:"; sed 's/^/  /' "$PLAN_CARGA"
  } > "$SAL/entorno.txt"
  cat "$SAL/entorno.txt" | tee -a "$SAL/logs/orquestador.log"
}

construir_imagenes() {
  for v in antes despues; do
    local d; d="$(dir_de "$v")"
    log "construyendo la imagen $v desde $(git -C "$d" rev-parse --short HEAD)"
    if ! docker build -f "$DESPUES_DIR/Dockerfile.measurement" -t "sv-backend:$v" "$d" > "$SAL/logs/build_$v.log" 2>&1; then
      tail -40 "$SAL/logs/build_$v.log" | tee -a "$SAL/logs/orquestador.log"
      fallo "no se pudo construir la imagen $v"
    fi
  done
}

levantar_postgres() {
  log "levantando PostgreSQL"
  docker network create sv-net > /dev/null
  docker run -d --name sv-pg --network sv-net $PIN_PG --memory 2g -e POSTGRES_PASSWORD="$PGPASS" postgres:16 \
    -c shared_preload_libraries=pg_stat_statements -c pg_stat_statements.track=all -c max_connections=100 > /dev/null
  for i in $(seq 1 60); do
    docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 && break
    sleep 2
  done
  docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 || fallo "PostgreSQL no arrancó"
  for v in antes despues; do
    docker exec sv-pg psql -U postgres -qc "CREATE DATABASE $v" || fallo "no se pudo crear la base $v"
  done
}

levantar_correo() {
  log "levantando el servidor SMTP de pruebas"
  docker run -d --name sv-correo --network sv-net -v "$RAIZ/medicion/escritura/sumidero_smtp.py:/sumidero.py:ro" python:3.12-alpine python -u /sumidero.py > /dev/null \
    || fallo "no se pudo levantar el servidor SMTP de pruebas"
  for i in $(seq 1 30); do
    docker logs sv-correo 2>&1 | grep -q '^LISTO' && break
    sleep 1
  done
  docker exec sv-correo python -c "
import smtplib
from email.message import EmailMessage
m = EmailMessage(); m['From'] = 'a@x.test'; m['To'] = 'b@x.test'; m['Subject'] = 'comprobacion'; m.set_content('hola')
with smtplib.SMTP('127.0.0.1', 2525, timeout=5) as s:
    s.ehlo(); s.send_message(m)
print('correo de comprobacion enviado')
" || fallo "el servidor SMTP de pruebas no responde"
}

correos_recibidos() { docker logs sv-correo 2>&1 | grep -c '^MSG'; }

iniciar_backend() {
  local v="$1" ddl="$2" cpus="$3" mem="$4" pin="$5"
  docker rm -f "sv-$v" > /dev/null 2>&1 || true
  docker run -d --name "sv-$v" --network sv-net $pin --cpus "$cpus" --memory "$mem" --memory-swap "$mem" -p "127.0.0.1:$PUERTO:8080" \
    -e DATABASE_URL="jdbc:postgresql://sv-pg:5432/$v" -e DB_USERNAME=postgres -e DB_PASSWORD="$PGPASS" -e DB_SSL_MODE=disable \
    -e DB_POOL_MAX_SIZE="$POOL" -e DB_POOL_MIN_IDLE=2 -e JPA_DDL_AUTO="$ddl" -e FLYWAY_ENABLED=false \
    -e JWT_PRIVATE_KEY=file:/app/keys/private_key.pem -e JWT_PUBLIC_KEY=file:/app/keys/public_key.pem \
    -e SUPABASE_URL=http://supabase-desactivado.local -e SUPABASE_SERVICE_ROLE_KEY=desactivado -e SUPABASE_BUCKET=desactivado \
    -e MAIL_HOST=sv-correo -e MAIL_PORT=2525 -e MAIL_USERNAME=desactivado -e MAIL_PASSWORD=desactivado -e MAIL_FROM=medicion@softvet.local \
    -e MANAGEMENT_HEALTH_MAIL_ENABLED=false -e APP_URL=http://localhost -e CORS_ALLOWED_ORIGINS=http://localhost \
    -e "JAVA_OPTS=$JAVA_OPTS_LAB" -e RATE_LIMIT_LOGIN_PER_MINUTE=100000 -e RATE_LIMIT_LOGIN_PER_ACCOUNT_PER_15_MINUTES=100000 \
    -e COOKIE_SECURE=false -e COOKIE_SAME_SITE=Lax -e TRUSTED_PROXY_COUNT=1 -e SWAGGER_ENABLED=false -e REMINDERS_CRON="0 0 0 1 1 *" \
    -e IA_RADIOGRAFIA_URL=http://ia-desactivada.local -e IA_LABORATORIO_URL=http://ia-desactivada.local -e IA_DIAGNOSTICO_URL=http://ia-desactivada.local \
    "sv-backend:$v" > /dev/null
}

esperar_salud() {
  local t0; t0="$(date +%s)"
  for i in $(seq 1 150); do
    if curl -fsS --max-time 2 "http://127.0.0.1:$PUERTO/api/v1/actuator/health" > /dev/null 2>&1; then
      echo $(( $(date +%s) - t0 ))
      return 0
    fi
    sleep 2
  done
  return 1
}

detener_backend() { docker logs "sv-$1" > "$SAL/logs/backend_$2.log" 2>&1; docker rm -f "sv-$1" > /dev/null 2>&1 || true; }

preparar_base() {
  local v="$1" d; d="$(dir_de "$v")"
  log "[$v] creando el esquema con el propio backend"
  iniciar_backend "$v" create 2 1g "--cpuset-cpus 1,3"
  esperar_salud > /dev/null || { docker logs "sv-$v" 2>&1 | tail -60 | tee -a "$SAL/logs/orquestador.log"; fallo "el backend $v no arrancó al crear el esquema"; }
  local espera=0
  until docker logs "sv-$v" 2>&1 | grep -q "INICIALIZACIÓN COMPLETADA"; do
    sleep 2; espera=$((espera+2))
    [ "$espera" -ge 120 ] && fallo "el DataInitializer de $v no terminó"
  done
  detener_backend "$v" "${v}_esquema"
  log "[$v] sembrando datos"
  psql_db "$v" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_stat_statements;" || fallo "extensiones en $v"
  psql_db "$v" -v citas_grande="${CITAS_GRANDE:-20000}" -v propietarios_grande="${PROPIETARIOS_GRANDE:-1200}" < "$RAIZ/medicion/sembrar.sql" > "$SAL/logs/siembra_$v.log" 2>&1 || { tail -30 "$SAL/logs/siembra_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la siembra de $v falló"; }
  : > "$SAL/logs/cuenta_$v.log"
  for n in $(seq 1 "$CUENTAS"); do
    psql_db "$v" -v measurement_company_slug="$SLUG" -v measurement_email="medicion$n@softvet.test" \
      -v measurement_username="$USUARIO$n" -v measurement_password="$CLAVE" < "$RAIZ/medicion/provisionar_cuenta.sql" \
      >> "$SAL/logs/cuenta_$v.log" 2>&1 || { tail -30 "$SAL/logs/cuenta_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la cuenta de medición $n en $v falló"; }
  done
  for f in "$d"/src/main/resources/db/historico/V53__*.sql "$d"/src/main/resources/db/historico/V76__*.sql; do
    psql_db "$v" < "$f" >> "$SAL/logs/siembra_$v.log" 2>&1 || fallo "tabla de seguridad $(basename "$f") en $v"
  done
  log "[$v] aplicando las migraciones de su versión"
  : > "$SAL/logs/migraciones_$v.log"
  local ok=0 mal=0
  for f in $(ls "$d"/src/main/resources/db/migration/V*.sql | sort -V); do
    case "$(basename "$f")" in V130__*) psql_db "$v" -c "DROP TABLE IF EXISTS consultation_field_answer, clinical_template_field, clinical_template_section, clinical_template, clinical_template_service, clinical_concept CASCADE; ALTER TABLE consulta DROP COLUMN IF EXISTS clinical_template_id;" >> "$SAL/logs/migraciones_$v.log" 2>&1 ;; esac
    if psql_db "$v" < "$f" >> "$SAL/logs/migraciones_$v.log" 2>&1; then ok=$((ok+1)); else mal=$((mal+1)); echo "ERROR en $(basename "$f")" >> "$SAL/logs/migraciones_$v.log"; fi
  done
  log "[$v] migraciones: $ok aplicadas, $mal con error"
  psql_db "$v" -c "ANALYZE" > /dev/null
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'citas='||(SELECT count(*) FROM citas)||' consultas='||(SELECT count(*) FROM consulta)||' mascotas='||(SELECT count(*) FROM mascota)" | tee -a "$SAL/logs/orquestador.log"
}

cpu_us() { docker exec "sv-$1" cat /sys/fs/cgroup/cpu.stat 2>/dev/null | awk '/^usage_usec/ {print $2}'; }

muestrear() {
  local contenedor="$1" archivo="$2"
  : > "$archivo"
  while true; do
    local linea
    linea="$(docker stats --no-stream --format '{{.CPUPerc}},{{.MemUsage}}' "$contenedor" 2>/dev/null || true)"
    [ -n "$linea" ] && echo "$(date +%s),$linea" >> "$archivo"
    sleep 4
  done
}

consulta_json() {
  docker exec sv-pg psql -U postgres -d "$1" -tAc "$2"
}

instantanea() {
  consulta_json "$1" "SELECT json_build_object(
    'apoderados',(SELECT count(*) FROM apoderado),
    'mascotas',(SELECT count(*) FROM mascota),
    'citas',(SELECT count(*) FROM citas),
    'citas_completadas',(SELECT count(*) FROM citas WHERE estado='COMPLETADA'),
    'citas_canceladas',(SELECT count(*) FROM citas WHERE estado='CANCELADA'),
    'consultas',(SELECT count(*) FROM consulta),
    'diagnosticos',(SELECT count(*) FROM diagnosticos),
    'recetas',(SELECT count(*) FROM prescripciones),
    'pagos',(SELECT count(*) FROM purchases),
    'usuarios',(SELECT count(*) FROM usuario))"
}

integridad() {
  consulta_json "$1" "SELECT json_build_object(
    'mascotas_sin_apoderado',(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL),
    'citas_sin_mascota',(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL),
    'consultas_sin_cita',(SELECT count(*) FROM consulta c LEFT JOIN citas ci ON ci.id=c.cita_id WHERE ci.id IS NULL),
    'documentos_repetidos',(SELECT count(*) FROM (SELECT numero_documento FROM apoderado WHERE company_id=$2 GROUP BY numero_documento HAVING count(*)>1) x))"
}

publicar_aviso() {
  local v="$1" jar base plantilla campos cuerpo
  jar="$(mktemp)"
  base="http://127.0.0.1:$PUERTO/api/v1"
  {
    echo "--- inicio de sesión del administrador de la clínica"
    curl -s -o /dev/null -w 'HTTP %{http_code}\n' -c "$jar" -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.1" \
      -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}1\",\"password\":\"$CLAVE\"}" "$base/auth/login"
    echo "--- plantilla del aviso de privacidad"
    plantilla="$(curl -s -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.1" "$base/admin/privacidad/aviso/plantilla?audiencia=PROPIETARIOS_Y_AUTORIZADOS")"
    echo "$plantilla" | cut -c1-600
    campos="$(echo "$plantilla" | jq -c '.data
      | .domicilio = (if (.domicilio // "") == "" then "Avenida Los Olivos 123" else .domicilio end)
      | .correoDerechos = (if (.correoDerechos // "") == "" then "derechos@clinica-medicion.softvet.test" else .correoDerechos end)
      | .transferencias = (if (.transferencias // "") == "" then "No se transfieren datos personales a terceros ni al extranjero" else .transferencias end)
      | .plazoConservacion = (if (.plazoConservacion // "") == "" then "Mientras dure la relación con la clínica y hasta 10 años por obligación legal" else .plazoConservacion end)')" || true
    echo "--- publicación del aviso"
    cuerpo="$(jq -nc --argjson c "${campos:-null}" '{audiencia:"PROPIETARIOS_Y_AUTORIZADOS", campos:$c, confirmoRevisionLegal:true}')"
    curl -s -b "$jar" -w '\nHTTP %{http_code}\n' -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.1" \
      -d "$cuerpo" "$base/admin/privacidad/aviso" | cut -c1-600
  } > "$SAL/logs/aviso_$v.log" 2>&1
  rm -f "$jar"
  log "[$v] aviso de privacidad: $(grep -E '^HTTP' "$SAL/logs/aviso_$v.log" | tr '\n' ' ')"
}

armar_ids() {
  local v="$1" company_id rol_cliente vets servicios raza_perro raza_gato
  company_id="$(consulta_json "$v" "SELECT id FROM company WHERE slug='$SLUG'")"
  rol_cliente="$(consulta_json "$v" "SELECT id FROM roles WHERE company_id=$company_id AND name='ROLE_CLIENTE'" | head -1)"
  vets="$(consulta_json "$v" "SELECT coalesce(json_agg(id ORDER BY id),'[]'::json) FROM empleado WHERE company_id=$company_id AND estado=true AND user_id IS NOT NULL")"
  servicios="$(consulta_json "$v" "SELECT coalesce(json_agg(json_build_object('id',id,'precio',precio) ORDER BY id),'[]'::json) FROM servicios WHERE company_id=$company_id AND activo=true AND disponible=true")"
  raza_perro="$(consulta_json "$v" "SELECT id FROM raza WHERE especie='PERRO' AND nombre='Mestizo' ORDER BY id LIMIT 1")"
  raza_gato="$(consulta_json "$v" "SELECT id FROM raza WHERE especie='GATO' AND nombre='Mestizo' ORDER BY id LIMIT 1")"
  [ -n "$rol_cliente" ] || fallo "no existe el rol ROLE_CLIENTE de la clínica en $v"
  jq -nc --argjson c "$company_id" --argjson r "$rol_cliente" --argjson v "$vets" --argjson s "$servicios" \
    --argjson p "${raza_perro:-0}" --argjson g "${raza_gato:-0}" \
    '{companyId:$c, rolCliente:$r, veterinarios:$v, servicios:$s, razas:{PERRO:$p, GATO:$g}, dias_con_turno:180, huecos_por_dia:16}'
}

esperar_inicializacion() {
  local v="$1" espera=0
  esperar_salud > /dev/null || { docker logs "sv-$v" 2>&1 | tail -40 | tee -a "$SAL/logs/orquestador.log"; fallo "el backend $v no arrancó"; }
  until docker logs "sv-$v" 2>&1 | grep -q "INICIALIZACIÓN COMPLETADA"; do
    sleep 2; espera=$((espera+2))
    [ "$espera" -ge 120 ] && fallo "el DataInitializer de $v no terminó"
  done
}

preparar_clinica() {
  local v="$1"
  opciones_jvm D
  log "[$v] configuración de la clínica (horarios, turnos, razas)"
  psql_db "$v" -v slug="$SLUG" < "$RAIZ/medicion/escritura/config_clinica.sql" > "$SAL/logs/config_$v.log" 2>&1 \
    || { tail -30 "$SAL/logs/config_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la configuración de la clínica en $v falló"; }
  if [ "$TOTAL_CUENTAS" -ge 2 ]; then
    log "[$v] cuentas de medición 2 a $TOTAL_CUENTAS"
    psql_db "$v" -v slug="$SLUG" -v clave="$CLAVE" -v desde=2 -v hasta="$TOTAL_CUENTAS" < "$RAIZ/medicion/escritura/provisionar_cuentas.sql" > "$SAL/logs/cuentas_$v.log" 2>&1 \
      || { tail -30 "$SAL/logs/cuentas_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "las cuentas de medición en $v fallaron"; }
  fi
  iniciar_backend "$v" none "$CPUS" "$MEMORIA" "$PIN_BACK"
  esperar_inicializacion "$v"
  publicar_aviso "$v"
  detener_backend "$v" "preparacion_$v"
}

correr_nivel_escritura() {
  local v="$1" nivel="$2" rampa="$3" mantener="$4" base_u="$5" base_cuenta="$6" config="${7:-D}" prefijo="${8:-$1}"
  local etiqueta="${prefijo}_${nivel}" etq_caja="$nivel" t0 salud ids codigo cpu0 cpu1 muestreador muestreador_pg correos0 correos1
  [ "$prefijo" != "$v" ] && etq_caja="${prefijo}-${nivel}"
  opciones_jvm "$config"
  log "[$v] nivel $nivel (config $config, $etiqueta): arrancando el backend ($CPUS CPU, $MEMORIA)"
  iniciar_backend "$v" none "$CPUS" "$MEMORIA" "$PIN_BACK"
  esperar_inicializacion "$v"
  salud="$(esperar_salud)"
  sleep 3
  ids="$(armar_ids "$v")"
  echo "$ids" > "$SAL/niveles/ids_${etiqueta}.json"
  instantanea "$v" > "$SAL/niveles/db_antes_${etiqueta}.json"
  docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT pg_stat_statements_reset()" > /dev/null
  cpu0="$(cpu_us "$v")"; cpu0="${cpu0:-0}"
  correos0="$(correos_recibidos)"
  muestrear "sv-$v" "$SAL/niveles/stats_${etiqueta}.csv" &
  muestreador=$!
  muestrear "sv-pg" "$SAL/niveles/stats_pg_${etiqueta}.csv" &
  muestreador_pg=$!
  t0="$(date +%s)"
  log "[$v] nivel $nivel: k6 con hasta $nivel usuarios (subida $rampa, sostenido $mantener)"
  $PIN_K6 env K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PERIOD=5s K6_WEB_DASHBOARD_EXPORT="$SAL/niveles/panel_k6_${etiqueta}.html" \
    k6 run --quiet --out "csv=$SAL/niveles/k6_${etiqueta}.csv" \
    -e BASE_URL="http://127.0.0.1:$PUERTO" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e IDS="$ids" \
    -e CUENTA_BASE="$base_cuenta" -e U_BASE="$base_u" -e ETIQUETA_NIVEL="$etq_caja" -e NIVEL="$nivel" -e RAMPA="$rampa" -e MANTENER="$mantener" \
    -e PAUSA_S="$PAUSA_S" -e TIMEOUT="$TIMEOUT_HTTP" -e SALIDA="$SAL/niveles/k6_${etiqueta}.json" \
    "$RAIZ/medicion/escritura/carga_escrituras.js" > "$SAL/logs/k6_${etiqueta}.log" 2>&1
  codigo=$?
  kill "$muestreador" "$muestreador_pg" 2> /dev/null; wait "$muestreador" "$muestreador_pg" 2> /dev/null
  local duracion=$(( $(date +%s) - t0 ))
  cpu1="$(cpu_us "$v")"; cpu1="${cpu1:-0}"
  correos1="$(correos_recibidos)"
  instantanea "$v" > "$SAL/niveles/db_despues_${etiqueta}.json"
  integridad "$v" "$(echo "$ids" | jq -r .companyId)" > "$SAL/niveles/integridad_${etiqueta}.json"
  local estado oom reinicios corriendo codigo_salida salud_final sql
  estado="$(docker inspect "sv-$v" --format '{{.State.OOMKilled}}|{{.RestartCount}}|{{.State.Running}}|{{.State.ExitCode}}' 2>/dev/null || echo 'desconocido|0|false|-1')"
  oom="${estado%%|*}"; estado="${estado#*|}"
  reinicios="${estado%%|*}"; estado="${estado#*|}"
  corriendo="${estado%%|*}"; codigo_salida="${estado#*|}"
  salud_final="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:$PUERTO/api/v1/actuator/health" || true)"
  sql="$(docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT coalesce(sum(calls),0)||'|'||coalesce(round(sum(total_exec_time)::numeric/1000,1),0) FROM pg_stat_statements WHERE dbid=(SELECT oid FROM pg_database WHERE datname='$v')" 2>/dev/null || echo '0|0')"
  jq -n --arg version "$v" --argjson nivel "$nivel" --argjson duracion "$duracion" \
    --argjson cpu_s "$(( (cpu1 - cpu0) / 1000000 ))" --arg oom "$oom" --argjson reinicios "${reinicios:-0}" \
    --arg corriendo "$corriendo" --argjson salida "${codigo_salida:--1}" --arg salud "$salud_final" \
    --arg sql "$sql" --argjson k6_codigo "$codigo" --arg config "$config" --argjson correos "$(( correos1 - correos0 ))" \
    '{version:$version, nivel:$nivel, config_jvm:$config, correos_recibidos:$correos, perfil:"carga_escritura", duracion_s:$duracion, cpu_contenedor_s:$cpu_s, oom_killed:($oom=="true"),
      reinicios:$reinicios, sigue_corriendo:($corriendo=="true"), codigo_salida:$salida, salud_final_http:$salud,
      k6_codigo_salida:$k6_codigo, sql_sentencias:(($sql|split("|")[0])|tonumber), sql_tiempo_s:(($sql|split("|")[1])|tonumber)}' \
    > "$SAL/niveles/estado_${etiqueta}.json" || log "no se pudo escribir el estado de $etiqueta"
  for registro in jvm_gc jvm_clases; do
    docker cp "sv-$v:/tmp/$registro.log" "$SAL/niveles/${registro}_${etiqueta}.log" > /dev/null 2>&1 \
      && gzip -f "$SAL/niveles/${registro}_${etiqueta}.log" \
      || log "[$v] no se pudo copiar /tmp/$registro.log de $etiqueta"
  done
  detener_backend "$v" "carga_${etiqueta}"
  gzip -f "$SAL/niveles/k6_${etiqueta}.csv" 2>/dev/null || true
  log "[$v] nivel $nivel terminado: k6 código $codigo, OOM=$oom, reinicios=$reinicios, salud final=$salud_final"
  sleep 20
}

PAUSA_S="${PAUSA_S:-15}"
CONTROL_USUARIOS=10
TOTAL_CUENTAS=$((1 + CONTROL_USUARIOS))
while read -r campo1 campo2 campo3 _; do
  case "$campo1" in ''|\#*) continue ;; esac
  TOTAL_CUENTAS=$((TOTAL_CUENTAS + campo1))
done < "$PLAN_CARGA"

registrar_entorno
construir_imagenes
levantar_postgres
levantar_correo
opciones_jvm D
preparar_base antes
preparar_base despues
for v in antes despues; do preparar_clinica "$v"; done

log "control: $CONTROL_USUARIOS usuarios con Metaspace de 128 MB (igual que el Dockerfile) y el servidor de correo de pruebas"
correr_nivel_escritura despues "$CONTROL_USUARIOS" 30s 150s 0 1 E ctrl128
python3 "$RAIZ/medicion/escritura/informe_carga.py" "$SAL" "$CONTROL_USUARIOS" control > "$SAL/logs/informe_control.log" 2>&1 \
  && cat "$SAL/niveles/INFORME_CONTROL_128.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
  || log "no se pudo generar el informe del control (ver logs/informe_control.log)"

base_u=$((CONTROL_USUARIOS * ITER_POR_VU))
base_cuenta=$((1 + CONTROL_USUARIOS))
while read -r nivel rampa mantener _ <&3; do
  case "$nivel" in ''|\#*) continue ;; esac
  for v in antes despues; do correr_nivel_escritura "$v" "$nivel" "$rampa" "$mantener" "$base_u" "$base_cuenta"; done
  python3 "$RAIZ/medicion/escritura/informe_carga.py" "$SAL" "$nivel" > "$SAL/logs/informe_carga_${nivel}.log" 2>&1 \
    && cat "$SAL/niveles/INFORME_CARGA_${nivel}.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el informe del nivel $nivel (ver logs/informe_carga_${nivel}.log)"
  base_u=$((base_u + nivel * ITER_POR_VU))
  base_cuenta=$((base_cuenta + nivel))
done 3< "$PLAN_CARGA"
python3 "$RAIZ/medicion/escritura/resumen_carga.py" "$SAL" > "$SAL/logs/resumen_carga.log" 2>&1 \
  && cat "$SAL/RESUMEN_FASE2B.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
  || log "no se pudo generar el resumen global (ver logs/resumen_carga.log)"
log "terminado"
