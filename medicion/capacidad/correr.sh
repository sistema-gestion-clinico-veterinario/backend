#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"
SAL="$RAIZ/resultados"
mkdir -p "$SAL/logs" "$SAL/niveles"

ANTES_DIR="${ANTES_DIR:-$RAIZ/.antes}"
DESPUES_DIR="${DESPUES_DIR:-$RAIZ/.despues}"
CONFIG="$(tr -d '[:space:]' < "$RAIZ/medicion/capacidad/CONFIG")"
PLAN="$RAIZ/medicion/capacidad/PLAN"
CPUS="${CPUS:-1}"
MEMORIA="${MEMORIA:-2g}"
CUENTAS="${CUENTAS:-50}"
PAUSA_S="${PAUSA_S:-15}"
TIMEOUT_HTTP="${TIMEOUT_HTTP:-30s}"
POOL="${POOL:-10}"
PGPASS="medicion"
PUERTO=18080
SLUG="clinica-medicion"
USUARIO="medicion"
CLAVE="$(openssl rand -hex 12)"

BASE_JVM="-XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m"
case "$CONFIG" in
  A) JAVA_OPTS_LAB="-Xmx192m $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
  B) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
  C) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m" ;;
  *) echo "CONFIG desconocida: '$CONFIG' (usa A, B o C)"; exit 1 ;;
esac

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
    echo "configuración de la JVM: $CONFIG -> $JAVA_OPTS_LAB"
    echo "pool de conexiones: $POOL"
    echo "pausa entre acciones: $PAUSA_S s (con variación aleatoria)"
    echo "cuentas de medición: $CUENTAS"
    echo "docker: $(docker version --format '{{.Server.Version}}')"
    echo "k6: $(k6 version | head -1)"
    echo "antes:   $(git -C "$ANTES_DIR" rev-parse HEAD)"
    echo "despues: $(git -C "$DESPUES_DIR" rev-parse HEAD)"
    echo "fijado de CPU: PG='$PIN_PG' backend='$PIN_BACK' k6='$PIN_K6'"
    echo "plan de niveles:"; sed 's/^/  /' "$PLAN"
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

iniciar_backend() {
  local v="$1" ddl="$2" cpus="$3" mem="$4" pin="$5"
  docker rm -f "sv-$v" > /dev/null 2>&1 || true
  docker run -d --name "sv-$v" --network sv-net $pin --cpus "$cpus" --memory "$mem" --memory-swap "$mem" -p "127.0.0.1:$PUERTO:8080" \
    -e DATABASE_URL="jdbc:postgresql://sv-pg:5432/$v" -e DB_USERNAME=postgres -e DB_PASSWORD="$PGPASS" -e DB_SSL_MODE=disable \
    -e DB_POOL_MAX_SIZE="$POOL" -e DB_POOL_MIN_IDLE=2 -e JPA_DDL_AUTO="$ddl" -e FLYWAY_ENABLED=false \
    -e JWT_PRIVATE_KEY=file:/app/keys/private_key.pem -e JWT_PUBLIC_KEY=file:/app/keys/public_key.pem \
    -e SUPABASE_URL=http://supabase-desactivado.local -e SUPABASE_SERVICE_ROLE_KEY=desactivado -e SUPABASE_BUCKET=desactivado \
    -e MAIL_HOST=127.0.0.1 -e MAIL_PORT=2525 -e MAIL_USERNAME=desactivado -e MAIL_PASSWORD=desactivado -e MAIL_FROM=medicion@softvet.local \
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
  psql_db "$v" < "$RAIZ/medicion/sembrar.sql" > "$SAL/logs/siembra_$v.log" 2>&1 || { tail -30 "$SAL/logs/siembra_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la siembra de $v falló"; }
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
    if psql_db "$v" < "$f" >> "$SAL/logs/migraciones_$v.log" 2>&1; then ok=$((ok+1)); else mal=$((mal+1)); echo "ERROR en $(basename "$f")" >> "$SAL/logs/migraciones_$v.log"; fi
  done
  log "[$v] migraciones: $ok aplicadas, $mal con error"
  psql_db "$v" -c "ANALYZE" > /dev/null
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'citas='||(SELECT count(*) FROM citas)||' consultas='||(SELECT count(*) FROM consulta)||' mascotas='||(SELECT count(*) FROM mascota)" | tee -a "$SAL/logs/orquestador.log"
}

cpu_us() { docker exec "sv-$1" cat /sys/fs/cgroup/cpu.stat 2>/dev/null | awk '/^usage_usec/ {print $2}'; }

muestrear() {
  local v="$1" archivo="$2"
  : > "$archivo"
  while true; do
    local linea
    linea="$(docker stats --no-stream --format '{{.CPUPerc}},{{.MemUsage}}' "sv-$v" 2>/dev/null || true)"
    [ -n "$linea" ] && echo "$(date +%s),$linea" >> "$archivo"
    sleep 4
  done
}

correr_nivel() {
  local v="$1" nivel="$2" rampa="$3" mantener="$4"
  local etiqueta="${v}_${nivel}" t0 salud company_id
  log "[$v] nivel $nivel: arrancando el backend ($CPUS CPU, $MEMORIA, configuración $CONFIG)"
  iniciar_backend "$v" none "$CPUS" "$MEMORIA" "$PIN_BACK"
  salud="$(esperar_salud)" || { docker logs "sv-$v" 2>&1 | tail -40 | tee -a "$SAL/logs/orquestador.log"; fallo "el backend $v no arrancó"; }
  log "[$v] arrancó en $salud s"
  sleep 3
  company_id="$(docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT id FROM company WHERE slug='$SLUG'")"
  docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT pg_stat_statements_reset()" > /dev/null
  local cpu0; cpu0="$(cpu_us "$v")"; cpu0="${cpu0:-0}"
  muestrear "$v" "$SAL/niveles/stats_${etiqueta}.csv" &
  local muestreador=$!
  t0="$(date +%s)"
  log "[$v] nivel $nivel: k6 con hasta $nivel usuarios (subida $rampa, sostenido $mantener)"
  $PIN_K6 k6 run --quiet --out "csv=$SAL/niveles/k6_${etiqueta}.csv" \
    -e BASE_URL="http://127.0.0.1:$PUERTO" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e COMPANY_ID="$company_id" \
    -e CUENTAS="$CUENTAS" -e PERFIL=capacidad -e NIVEL="$nivel" -e RAMPA="$rampa" -e MANTENER="$mantener" \
    -e PAUSA_S="$PAUSA_S" -e JITTER=1 -e TIMEOUT="$TIMEOUT_HTTP" -e SALIDA="$SAL/niveles/k6_${etiqueta}.json" \
    "$RAIZ/medicion/escenario_p1_p4.js" > "$SAL/logs/k6_${etiqueta}.log" 2>&1
  local codigo=$?
  kill "$muestreador" 2> /dev/null; wait "$muestreador" 2> /dev/null
  local duracion=$(( $(date +%s) - t0 ))
  local cpu1; cpu1="$(cpu_us "$v")"; cpu1="${cpu1:-0}"
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
    --arg sql "$sql" --argjson arranque "$salud" --argjson k6_codigo "$codigo" \
    '{version:$version, nivel:$nivel, duracion_s:$duracion, cpu_contenedor_s:$cpu_s, oom_killed:($oom=="true"),
      reinicios:$reinicios, sigue_corriendo:($corriendo=="true"), codigo_salida:$salida, salud_final_http:$salud,
      arranque_s:$arranque, k6_codigo_salida:$k6_codigo, sql_sentencias:(($sql|split("|")[0])|tonumber), sql_tiempo_s:(($sql|split("|")[1])|tonumber)}' \
    > "$SAL/niveles/estado_${etiqueta}.json" || log "no se pudo escribir el estado de $etiqueta"
  detener_backend "$v" "$etiqueta"
  gzip -f "$SAL/niveles/k6_${etiqueta}.csv" 2>/dev/null || true
  log "[$v] nivel $nivel terminado: k6 código $codigo, OOM=$oom, reinicios=$reinicios, salud final=$salud_final"
  sleep 20
}

registrar_entorno
construir_imagenes
levantar_postgres
preparar_base antes
preparar_base despues

while read -r nivel rampa mantener _; do
  case "$nivel" in ''|\#*) continue ;; esac
  for v in antes despues; do correr_nivel "$v" "$nivel" "$rampa" "$mantener"; done
  python3 "$RAIZ/medicion/capacidad/informe_nivel.py" "$SAL" "$nivel" > "$SAL/logs/informe_$nivel.log" 2>&1 \
    && cat "$SAL/niveles/INFORME_${nivel}.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el informe del nivel $nivel (ver logs/informe_$nivel.log)"
done < "$PLAN"
log "terminado"
