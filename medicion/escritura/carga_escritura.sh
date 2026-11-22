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
PLAN_CARGA="${PLAN_CARGA:-$RAIZ/medicion/escritura/PLAN_CARGA}"
VERSIONES="${VERSIONES:-antes despues}"
SIN_CONTROL="${SIN_CONTROL:-0}"
VARIANTES="${VARIANTES:-}"
ENV_EXTRA_BACKEND=""
RESET_BASE="${RESET_BASE:-0}"
MODO_VARIANTES="${MODO_VARIANTES:-xms}"
XMX="${XMX:-1g}"
K6_NUCLEOS="${K6_NUCLEOS:-0,3}"
ITER_POR_VU="${ITER_POR_VU:-19}"
PAUSA_S="${PAUSA_S:-15}"
PESOS="${PESOS:-30,35,30,5}"
TIMEOUT_HTTP="${TIMEOUT_HTTP:-30s}"
POOL="${POOL:-10}"
LIMITE_LOGIN_IP="${LIMITE_LOGIN_IP:-100000}"
LIMITE_LOGIN_CUENTA="${LIMITE_LOGIN_CUENTA:-100000}"
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
    D) JAVA_OPTS_LAB="-Xmx$XMX -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    E) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    F) JAVA_OPTS_LAB="-Xms$XMX -Xmx$XMX -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    G) JAVA_OPTS_LAB="-Xms${XMX_G:-1280m} -Xmx${XMX_G:-1280m} -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    *) echo "configuración desconocida: '$1' (usa A, B, C, D, E, F o G)"; exit 1 ;;
  esac
}
opciones_jvm "$CONFIG"

NUCLEOS="$(nproc)"
if [ "$NUCLEOS" -ge 4 ]; then
  PIN_PG="--cpuset-cpus 2"
  if [ "$CPUS" -ge 2 ]; then PIN_BACK="--cpuset-cpus 1,3"; else PIN_BACK="--cpuset-cpus 1"; fi
  PIN_K6="taskset -c $K6_NUCLEOS"
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
    echo "procesador: $(lscpu | awk -F: '/Model name/ {gsub(/^ +/,"",$2); print $2}'); hilos por núcleo: $(lscpu | awk -F: '/Thread\(s\) per core/ {gsub(/ /,"",$2); print $2}'); núcleos por socket: $(lscpu | awk -F: '/Core\(s\) per socket/ {gsub(/ /,"",$2); print $2}')"
    [ -n "$VARIANTES" ] && echo "variantes (config:etiqueta), el orden alterna por nivel: $VARIANTES"
    echo "versiones medidas: $VERSIONES; heap máximo: $XMX; núcleos de k6: $K6_NUCLEOS; reservas de horario por usuario: $ITER_POR_VU"
    free -m | sed -n 2p | awk '{print "memoria total del equipo (MB): "$2}'
    echo "recursos del backend: $CPUS CPU, $MEMORIA de RAM"
    echo "configuraciones de la JVM:"
    for c in C D E F G; do opciones_jvm "$c"; echo "  $c -> $JAVA_OPTS_LAB"; done
    echo "usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control"
    echo "correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)"
    opciones_jvm D
    echo "pool de conexiones: $POOL"
    echo "pausa entre acciones: $PAUSA_S s (con variación aleatoria)"
    echo "mezcla de acciones (panel, agenda, historia, reportes, recepción, atención, cobro, cancelar): 22,18,15,2,13,12,10,8 %"
    echo "cuentas de medición: una por usuario virtual, $TOTAL_CUENTAS en total"
    echo "docker: $(docker version --format '{{.Server.Version}}')"
    echo "k6: $(k6 version | head -1)"
    echo "antes:   $(git -C "$ANTES_DIR" rev-parse HEAD 2>/dev/null || echo no medido)"
    echo "despues: $(git -C "$DESPUES_DIR" rev-parse HEAD 2>/dev/null || echo no medido)"
    echo "fijado de CPU: PG='$PIN_PG' backend='$PIN_BACK' k6='$PIN_K6'"
    echo "plan de niveles:"; sed 's/^/  /' "$PLAN_CARGA"
  } > "$SAL/entorno.txt"
  cat "$SAL/entorno.txt" | tee -a "$SAL/logs/orquestador.log"
}

construir_imagenes() {
  for v in $VERSIONES; do
    local d; d="$(dir_de "$v")"
    log "construyendo la imagen $v desde $(git -C "$d" rev-parse --short HEAD)"
    local intento construida=0
    for intento in 1 2 3 4; do
      if docker build -f "$DESPUES_DIR/Dockerfile.measurement" -t "sv-backend:$v" "$d" > "$SAL/logs/build_$v.log" 2>&1; then construida=1; break; fi
      log "la construcción de $v falló (intento $intento de 4); espera de $((intento * 60)) s antes de reintentar"
      sleep $((intento * 60))
    done
    if [ "$construida" != 1 ]; then
      tail -40 "$SAL/logs/build_$v.log" | tee -a "$SAL/logs/orquestador.log"
      fallo "no se pudo construir la imagen $v"
    fi
  done
}

levantar_postgres() {
  log "levantando PostgreSQL"
  docker network create sv-net > /dev/null
  docker run -d --name sv-pg --network sv-net $PIN_PG --memory 2g -e POSTGRES_PASSWORD="$PGPASS" postgres:16 \
    -c shared_preload_libraries=pg_stat_statements,auto_explain -c pg_stat_statements.track=all -c max_connections=100 \
    -c auto_explain.log_min_duration=300 -c auto_explain.log_analyze=on -c auto_explain.log_buffers=on -c auto_explain.sample_rate=0.1 ${PG_SHARED_BUFFERS:+-c shared_buffers=$PG_SHARED_BUFFERS} > /dev/null
  for i in $(seq 1 60); do
    docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 && break
    sleep 2
  done
  docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 || fallo "PostgreSQL no arrancó"
  for v in $VERSIONES; do
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
    -e "JAVA_OPTS=$JAVA_OPTS_LAB" -e RATE_LIMIT_LOGIN_PER_MINUTE="$LIMITE_LOGIN_IP" -e RATE_LIMIT_LOGIN_PER_ACCOUNT_PER_15_MINUTES="$LIMITE_LOGIN_CUENTA" \
    -e COOKIE_SECURE=false -e COOKIE_SAME_SITE=Lax -e TRUSTED_PROXY_COUNT=1 -e SWAGGER_ENABLED=false -e REMINDERS_CRON="0 0 0 1 1 *" \
    -e IA_RADIOGRAFIA_URL=http://ia-desactivada.local -e IA_LABORATORIO_URL=http://ia-desactivada.local -e IA_DIAGNOSTICO_URL=http://ia-desactivada.local \
    ${ENV_EXTRA_BACKEND:+-e $ENV_EXTRA_BACKEND} \
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

crear_plantilla() {
  local v="$1"
  log "[$v] guardando la base preparada como plantilla"
  docker exec sv-pg psql -U postgres -qc "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$v' AND pid <> pg_backend_pid()" > /dev/null 2>&1 || true
  docker exec sv-pg psql -U postgres -qc "CREATE DATABASE plantilla TEMPLATE $v" || fallo "no se pudo crear la plantilla de la base"
}

reiniciar_base() {
  local v="$1" prep="$2" etiqueta="$3"
  docker exec sv-pg psql -U postgres -qc "DROP DATABASE IF EXISTS $v WITH (FORCE)" || fallo "no se pudo borrar la base $v"
  docker exec sv-pg psql -U postgres -qc "CREATE DATABASE $v TEMPLATE plantilla" || fallo "no se pudo recrear la base $v desde la plantilla"
  if [ -n "$prep" ]; then
    psql_db "$v" < "$RAIZ/medicion/escritura/$prep" > "$SAL/logs/preparacion_${etiqueta}.log" 2>&1 || fallo "la preparación $prep de $etiqueta falló"
    log "[$v] preparación aplicada a $etiqueta: $prep"
  fi
}

sonda_panel() {
  local v="$1" etiqueta="$2" ids="$3" jar base cid n=20 i r codigo segundos ok=0 primera="" tiempos="" resp sql
  jar="$(mktemp)"; resp="$(mktemp)"
  base="http://127.0.0.1:$PUERTO/api/v1"
  cid="$(echo "$ids" | jq -r .companyId)"
  curl -s -o /dev/null -c "$jar" -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.2" \
    -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}1\",\"password\":\"$CLAVE\"}" "$base/auth/login"
  r="$(curl -s -o "$SAL/niveles/panel_respuesta_${etiqueta}.json" -w '%{http_code} %{time_total}' -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.2" "$base/dashboard/overview?companyId=$cid")"
  primera="$(echo "$r" | awk '{printf "%d", $2 * 1000}')"
  docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT pg_stat_statements_reset()" > /dev/null
  for i in $(seq 2 $n); do
    r="$(curl -s -o "$resp" -w '%{http_code} %{time_total}' -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.2" "$base/dashboard/overview?companyId=$cid")"
    codigo="${r%% *}"; segundos="${r##* }"
    [ "$codigo" = "200" ] && ok=$((ok + 1))
    tiempos="$tiempos$(echo "$segundos" | awk '{printf "%d", $1 * 1000}'),"
  done
  sql="$(docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT coalesce(sum(calls), 0) || '|' || coalesce(round(sum(total_exec_time)::numeric, 1), 0) FROM pg_stat_statements WHERE dbid = (SELECT oid FROM pg_database WHERE datname = '$v') AND query NOT ILIKE '%pg_stat_statements%'" 2> /dev/null || echo '0|0')"
  jq -n --argjson llamadas "$((n - 1))" --argjson ok "$ok" --argjson primera "${primera:-0}" --arg sql "$sql" --arg tiempos "${tiempos%,}" \
    '{llamadas:$llamadas, http_200:$ok, primera_llamada_ms:$primera, sentencias_sql:(($sql|split("|")[0])|tonumber), tiempo_sql_ms:(($sql|split("|")[1])|tonumber), tiempos_ms:($tiempos|split(",")|map(tonumber))}' \
    > "$SAL/niveles/sonda_panel_${etiqueta}.json" || log "[$v] no se pudo escribir la sonda del panel de $etiqueta"
  rm -f "$jar" "$resp"
}

info_consentimiento() {
  docker exec sv-pg psql -U postgres -d "$1" -tAc "SELECT json_build_object('filas', (SELECT count(*) FROM consentimiento_datos), 'usuarios', (SELECT count(DISTINCT usuario_id) FROM consentimiento_datos), 'tabla_mb', (SELECT round(pg_total_relation_size('consentimiento_datos') / 1048576.0, 1)), 'indices', (SELECT coalesce(json_agg(indexdef), '[]'::json) FROM pg_indexes WHERE tablename = 'consentimiento_datos'))"
}

caos_en_segundo_plano() {
  local v="$1" etiqueta="$2" evento="$3" espera="$4"
  local t_inicio t_evento t_bd_listo="" t_backend_ok="" i
  t_inicio="$(date +%s.%N)"
  sleep "$espera"
  t_evento="$(date +%s.%N)"
  case "$evento" in
    bd_reinicio) docker restart sv-pg > /dev/null 2>&1 ;;
    bd_kill) docker kill sv-pg > /dev/null 2>&1; docker start sv-pg > /dev/null 2>&1 ;;
    backend_kill) docker kill "sv-$v" > /dev/null 2>&1; docker start "sv-$v" > /dev/null 2>&1 ;;
  esac
  case "$evento" in
    bd_*)
      for i in $(seq 1 300); do
        docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 && { t_bd_listo="$(date +%s.%N)"; break; }
        sleep 0.5
      done ;;
  esac
  for i in $(seq 1 600); do
    if curl -fsS --max-time 2 "http://127.0.0.1:$PUERTO/api/v1/actuator/health" > /dev/null 2>&1; then t_backend_ok="$(date +%s.%N)"; break; fi
    sleep 0.5
  done
  jq -n --arg evento "$evento" --arg inicio "$t_inicio" --arg evento_t "$t_evento" --arg bd "$t_bd_listo" --arg be "$t_backend_ok" \
    '{evento:$evento, t_inicio_k6:($inicio|tonumber), t_evento:($evento_t|tonumber), t_bd_listo:(if $bd=="" then null else ($bd|tonumber) end), t_backend_ok:(if $be=="" then null else ($be|tonumber) end)}' \
    > "$SAL/niveles/caos_${etiqueta}.json"
}

correr_nivel_escritura() {
  local v="$1" nivel="$2" rampa="$3" mantener="$4" base_u="$5" base_cuenta="$6" config="${7:-D}" prefijo="${8:-$1}" prep="${9:-}" envx="${10:-}" inicio_iso
  local etiqueta="${prefijo}_${nivel}" etq_caja="$nivel" t0 salud ids codigo cpu0 cpu1 muestreador muestreador_pg correos0 correos1
  [ "$prefijo" != "$v" ] && etq_caja="${prefijo}-${nivel}"
  opciones_jvm "$config"
  ENV_EXTRA_BACKEND="$envx"
  if [ "$RESET_BASE" = 1 ]; then reiniciar_base "$v" "$prep" "$etiqueta"; fi
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'plan_cache_mode=' || current_setting('plan_cache_mode') || ' jit=' || current_setting('jit')" > "$SAL/niveles/config_bd_${etiqueta}.txt" 2> /dev/null || true
  log "[$v] nivel $nivel (config $config, $etiqueta): arrancando el backend ($CPUS CPU, $MEMORIA)"
  iniciar_backend "$v" none "$CPUS" "$MEMORIA" "$PIN_BACK"
  esperar_inicializacion "$v"
  salud="$(esperar_salud)"
  sleep 3
  ids="$(armar_ids "$v")"
  echo "$ids" > "$SAL/niveles/ids_${etiqueta}.json"
  instantanea "$v" > "$SAL/niveles/db_antes_${etiqueta}.json"
  sonda_panel "$v" "$etiqueta" "$ids"
  info_consentimiento "$v" > "$SAL/niveles/consentimiento_antes_${etiqueta}.json" 2> /dev/null || true
  docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT pg_stat_statements_reset()" > /dev/null
  cpu0="$(cpu_us "$v")"; cpu0="${cpu0:-0}"
  correos0="$(correos_recibidos)"
  muestrear "sv-$v" "$SAL/niveles/stats_${etiqueta}.csv" &
  muestreador=$!
  muestrear "sv-pg" "$SAL/niveles/stats_pg_${etiqueta}.csv" &
  muestreador_pg=$!
  t0="$(date +%s)"
  inicio_iso="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  log "[$v] nivel $nivel: k6 con hasta $nivel usuarios (subida $rampa, sostenido $mantener)"
  local evento_caos="" pid_caos=""
  case "$prefijo" in caos_*) evento_caos="${prefijo#caos_}" ;; esac
  if [ -n "$evento_caos" ]; then
    log "[$v] evento de caos programado: $evento_caos a los ${ESPERA_CAOS:-150} s del inicio de la carga"
    caos_en_segundo_plano "$v" "$etiqueta" "$evento_caos" "${ESPERA_CAOS:-150}" &
    pid_caos=$!
  fi
  $PIN_K6 env K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PERIOD=5s K6_WEB_DASHBOARD_EXPORT="$SAL/niveles/panel_k6_${etiqueta}.html" \
    k6 run --quiet --out "csv=$SAL/niveles/k6_${etiqueta}.csv" \
    -e BASE_URL="http://127.0.0.1:$PUERTO" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e IDS="$ids" \
    -e CUENTA_BASE="$base_cuenta" -e U_BASE="$base_u" -e ITER_POR_VU="$ITER_POR_VU" -e ETIQUETA_NIVEL="$etq_caja" -e NIVEL="$nivel" -e RAMPA="$rampa" -e MANTENER="$mantener" \
    -e PAUSA_S="$PAUSA_S" -e TIMEOUT="$TIMEOUT_HTTP" -e SALIDA="$SAL/niveles/k6_${etiqueta}.json" \
    "$RAIZ/medicion/escritura/carga_escrituras.js" > "$SAL/logs/k6_${etiqueta}.log" 2>&1
  codigo=$?
  [ -n "$pid_caos" ] && wait "$pid_caos"
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
    --arg heap_max "$XMX" --argjson cpus_limite "$CPUS" --argjson memoria_limite_mb "$(( ${MEMORIA%g} * 1024 ))" \
    '{version:$version, nivel:$nivel, config_jvm:$config, correos_recibidos:$correos, heap_max:$heap_max, cpus_limite:$cpus_limite, memoria_limite_mb:$memoria_limite_mb, perfil:"carga_escritura", duracion_s:$duracion, cpu_contenedor_s:$cpu_s, oom_killed:($oom=="true"),
      reinicios:$reinicios, sigue_corriendo:($corriendo=="true"), codigo_salida:$salida, salud_final_http:$salud,
      k6_codigo_salida:$k6_codigo, sql_sentencias:(($sql|split("|")[0])|tonumber), sql_tiempo_s:(($sql|split("|")[1])|tonumber)}' \
    > "$SAL/niveles/estado_${etiqueta}.json" || log "no se pudo escribir el estado de $etiqueta"
  for registro in jvm_gc jvm_clases; do
    docker cp "sv-$v:/tmp/$registro.log" "$SAL/niveles/${registro}_${etiqueta}.log" > /dev/null 2>&1 \
      && gzip -f "$SAL/niveles/${registro}_${etiqueta}.log" \
      || log "[$v] no se pudo copiar /tmp/$registro.log de $etiqueta"
  done
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT coalesce(json_agg(t), '[]'::json) FROM (SELECT left(regexp_replace(query, '[[:space:]]+', ' ', 'g'), 220) AS consulta, calls, round(total_exec_time::numeric, 0) AS total_ms, round(mean_exec_time::numeric, 2) AS media_ms, rows FROM pg_stat_statements WHERE dbid = (SELECT oid FROM pg_database WHERE datname = '$v') ORDER BY total_exec_time DESC LIMIT 10) t" > "$SAL/niveles/sql_top_${etiqueta}.json" 2> /dev/null \
    || log "[$v] no se pudo volcar las consultas más costosas de $etiqueta"
  info_consentimiento "$v" > "$SAL/niveles/consentimiento_despues_${etiqueta}.json" 2> /dev/null || true
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT json_build_object('llamadas', coalesce(sum(calls), 0), 'total_ms', coalesce(round(sum(total_exec_time)::numeric, 0), 0), 'media_ms', coalesce(round((sum(total_exec_time) / nullif(sum(calls), 0))::numeric, 2), 0)) FROM pg_stat_statements WHERE dbid = (SELECT oid FROM pg_database WHERE datname = '$v') AND query ILIKE '%consentimiento_datos%'" > "$SAL/niveles/sql_consentimiento_${etiqueta}.json" 2> /dev/null || true
  docker logs --since "$inicio_iso" sv-pg 2>&1 | head -c 600000 | gzip > "$SAL/niveles/plan_pg_${etiqueta}.log.gz" || true
  detener_backend "$v" "carga_${etiqueta}"
  ENV_EXTRA_BACKEND=""
  gzip -f "$SAL/niveles/k6_${etiqueta}.csv" 2>/dev/null || true
  log "[$v] nivel $nivel terminado: k6 código $codigo, OOM=$oom, reinicios=$reinicios, salud final=$salud_final"
  sleep 20
}

PAUSA_S="${PAUSA_S:-15}"
CONTROL_USUARIOS=10
[ "$SIN_CONTROL" = 1 ] && CONTROL_USUARIOS=0
MODO_INFORME=""
[ "$VERSIONES" = "despues" ] && MODO_INFORME="una"
[ -n "$VARIANTES" ] && MODO_INFORME="$MODO_VARIANTES"
[ "$MODO_VARIANTES" = "caos" ] && MODO_INFORME="multi"
RESUMEN_SCRIPT="resumen_xms.py"
[ "$MODO_VARIANTES" = "caos" ] && RESUMEN_SCRIPT="resumen_caos.py"
[ "$MODO_VARIANTES" = "multi" ] && RESUMEN_SCRIPT="resumen_multi.py"
MULTIPLICADOR=1
[ -n "$VARIANTES" ] && MULTIPLICADOR=$(echo "$VARIANTES" | wc -w)
TOTAL_CUENTAS=$((1 + CONTROL_USUARIOS))
maximo_nivel=0
while read -r campo1 campo2 campo3 _; do
  case "$campo1" in ''|\#*) continue ;; esac
  if [ "$RESET_BASE" = 1 ]; then
    [ "$campo1" -gt "$maximo_nivel" ] && maximo_nivel=$campo1
  else
    TOTAL_CUENTAS=$((TOTAL_CUENTAS + campo1 * MULTIPLICADOR))
  fi
done < "$PLAN_CARGA"
[ "$RESET_BASE" = 1 ] && TOTAL_CUENTAS=$((1 + CONTROL_USUARIOS + maximo_nivel))

registrar_entorno
construir_imagenes
levantar_postgres
levantar_correo
opciones_jvm D
for v in $VERSIONES; do preparar_base "$v"; done
if [ "${MODO_SEGURIDAD:-0}" = 1 ]; then
  for v in $VERSIONES; do
    log "[$v] prueba de seguridad con PostgreSQL real"
    iniciar_backend "$v" none 1 2g "$PIN_BACK"
    esperar_salud > /dev/null || fallo "el backend $v no arrancó"
    esperar_inicializacion "$v"
    export PUERTO SLUG USUARIO CLAVE SAL POOL LIMITE_LOGIN_IP LIMITE_LOGIN_CUENTA
    bash "$RAIZ/medicion/escritura/prueba_seguridad.sh" "$v" || true
    cat "$SAL/seguridad_$v.txt" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    detener_backend "$v" "seguridad_$v"
  done
  log "terminado (prueba de seguridad)"
  exit 0
fi
for v in $VERSIONES; do preparar_clinica "$v"; done
[ "$RESET_BASE" = 1 ] && crear_plantilla despues

if [ "$SIN_CONTROL" != 1 ]; then
  log "control: $CONTROL_USUARIOS usuarios con Metaspace de 128 MB (igual que el Dockerfile) y el servidor de correo de pruebas"
  correr_nivel_escritura despues "$CONTROL_USUARIOS" 30s 150s 0 1 E ctrl128
  python3 "$RAIZ/medicion/escritura/informe_carga.py" "$SAL" "$CONTROL_USUARIOS" control > "$SAL/logs/informe_control.log" 2>&1 \
    && cat "$SAL/niveles/INFORME_CONTROL_128.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el informe del control (ver logs/informe_control.log)"
fi

base_u=$((CONTROL_USUARIOS * ITER_POR_VU))
base_cuenta=$((1 + CONTROL_USUARIOS))
indice=0
while read -r nivel rampa mantener _ <&3; do
  case "$nivel" in ''|\#*) continue ;; esac
  if [ -n "$VARIANTES" ]; then
    orden="$VARIANTES"
    if [ $((indice % 2)) -eq 1 ]; then orden="$(echo "$VARIANTES" | awk '{for (i = NF; i > 0; i--) printf "%s%s", $i, (i > 1 ? " " : "")}')"; fi
    for variante in $orden; do
      IFS=: read -r cfg_variante pref_variante prep_variante env_variante version_variante <<< "$variante"
      correr_nivel_escritura "${version_variante:-despues}" "$nivel" "$rampa" "$mantener" "$base_u" "$base_cuenta" "$cfg_variante" "$pref_variante" "${prep_variante:-}" "${env_variante:-}"
      if [ "$RESET_BASE" != 1 ]; then
        base_u=$((base_u + nivel * ITER_POR_VU))
        base_cuenta=$((base_cuenta + nivel))
      fi
    done
    indice=$((indice + 1))
  else
    for v in $VERSIONES; do correr_nivel_escritura "$v" "$nivel" "$rampa" "$mantener" "$base_u" "$base_cuenta"; done
  fi
  python3 "$RAIZ/medicion/escritura/informe_carga.py" "$SAL" "$nivel" $MODO_INFORME > "$SAL/logs/informe_carga_${nivel}.log" 2>&1 \
    && cat "$SAL/niveles/INFORME_CARGA_${nivel}.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el informe del nivel $nivel (ver logs/informe_carga_${nivel}.log)"
  if [ -z "$VARIANTES" ]; then
    base_u=$((base_u + nivel * ITER_POR_VU))
    base_cuenta=$((base_cuenta + nivel))
  fi
done 3< "$PLAN_CARGA"
if [ -n "$VARIANTES" ]; then
  python3 "$RAIZ/medicion/escritura/$RESUMEN_SCRIPT" "$SAL" "$MODO_VARIANTES" > "$SAL/logs/resumen_xms.log" 2>&1 \
    && cat "$SAL"/RESUMEN_FASE3*.md >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el resumen de la fase 3b (ver logs/resumen_xms.log)"
elif [ "$VERSIONES" = "despues" ]; then
  python3 "$RAIZ/medicion/escritura/resumen_escalado.py" "$SAL" > "$SAL/logs/resumen_escalado.log" 2>&1 \
    && cat "$SAL/RESUMEN_FASE3.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el resumen del escalado (ver logs/resumen_escalado.log)"
else
  python3 "$RAIZ/medicion/escritura/resumen_carga.py" "$SAL" > "$SAL/logs/resumen_carga.log" 2>&1 \
    && cat "$SAL/RESUMEN_FASE2B.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el resumen global (ver logs/resumen_carga.log)"
fi
log "terminado"
