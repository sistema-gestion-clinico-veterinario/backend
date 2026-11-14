#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"
SAL="$RAIZ/resultados"
mkdir -p "$SAL/corridas" "$SAL/logs"

ANTES_DIR="${ANTES_DIR:-$RAIZ/.antes}"
DESPUES_DIR="${DESPUES_DIR:-$RAIZ/.despues}"
RONDAS="${RONDAS:-5}"
REPOSO_S="${REPOSO_S:-30}"
VUS="${VUS:-2}"
ITER="${ITER:-10}"
PAUSA_S="${PAUSA_S:-0.3}"
PGPASS="medicion"
PUERTO=18080
SLUG="clinica-medicion"
USUARIO="medicion"
CLAVE="$(openssl rand -hex 12)"

NUCLEOS="$(nproc)"
if [ "$NUCLEOS" -ge 4 ]; then
  PIN_PG="--cpuset-cpus 0"
  PIN_BACK="--cpuset-cpus 1,2"
  PIN_K6="taskset -c 3"
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
    echo "núcleos: $NUCLEOS"
    free -m | sed -n 2p | awk '{print "memoria total (MB): "$2}'
    echo "núcleo del sistema: $(uname -r)"
    echo "docker: $(docker version --format '{{.Server.Version}}')"
    echo "k6: $(k6 version | head -1)"
    echo "antes:   $(git -C "$ANTES_DIR" rev-parse HEAD) ($(git -C "$ANTES_DIR" log -1 --format=%s | cut -c1-80))"
    echo "despues: $(git -C "$DESPUES_DIR" rev-parse HEAD) ($(git -C "$DESPUES_DIR" log -1 --format=%s | cut -c1-80))"
    echo "fijado de CPU: PG='$PIN_PG' backend='$PIN_BACK' k6='$PIN_K6'"
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
  local v="$1" ddl="$2"
  docker rm -f "sv-$v" > /dev/null 2>&1 || true
  docker run -d --name "sv-$v" --network sv-net $PIN_BACK --cpus 2 --memory 768m -p "127.0.0.1:$PUERTO:8080" \
    -e DATABASE_URL="jdbc:postgresql://sv-pg:5432/$v" -e DB_USERNAME=postgres -e DB_PASSWORD="$PGPASS" -e DB_SSL_MODE=disable \
    -e DB_POOL_MAX_SIZE=10 -e DB_POOL_MIN_IDLE=2 -e JPA_DDL_AUTO="$ddl" -e FLYWAY_ENABLED=false \
    -e JWT_PRIVATE_KEY=file:/app/keys/private_key.pem -e JWT_PUBLIC_KEY=file:/app/keys/public_key.pem \
    -e SUPABASE_URL=http://supabase-desactivado.local -e SUPABASE_SERVICE_ROLE_KEY=desactivado -e SUPABASE_BUCKET=desactivado \
    -e MAIL_HOST=127.0.0.1 -e MAIL_PORT=2525 -e MAIL_USERNAME=desactivado -e MAIL_PASSWORD=desactivado -e MAIL_FROM=medicion@softvet.local \
    -e MANAGEMENT_HEALTH_MAIL_ENABLED=false -e APP_URL=http://localhost -e CORS_ALLOWED_ORIGINS=http://localhost \
    -e COOKIE_SECURE=false -e COOKIE_SAME_SITE=Lax -e TRUSTED_PROXY_COUNT=0 -e SWAGGER_ENABLED=false -e REMINDERS_CRON="0 0 0 1 1 *" \
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

detener_backend() { docker logs "sv-$1" > "$SAL/logs/backend_$1_$2.log" 2>&1; docker rm -f "sv-$1" > /dev/null 2>&1 || true; }

preparar_base() {
  local v="$1" d; d="$(dir_de "$v")"
  log "[$v] creando el esquema con el propio backend"
  iniciar_backend "$v" create
  esperar_salud > /dev/null || { docker logs "sv-$v" 2>&1 | tail -60 | tee -a "$SAL/logs/orquestador.log"; fallo "el backend $v no arrancó al crear el esquema"; }
  detener_backend "$v" esquema
  log "[$v] sembrando datos"
  psql_db "$v" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto; CREATE EXTENSION IF NOT EXISTS pg_stat_statements;" || fallo "extensiones en $v"
  psql_db "$v" < "$RAIZ/medicion/sembrar.sql" > "$SAL/logs/siembra_$v.log" 2>&1 || { tail -30 "$SAL/logs/siembra_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la siembra de $v falló"; }
  psql_db "$v" -v measurement_company_slug="$SLUG" -v measurement_email="medicion@softvet.test" \
    -v measurement_username="$USUARIO" -v measurement_password="$CLAVE" < "$RAIZ/medicion/provisionar_cuenta.sql" \
    > "$SAL/logs/cuenta_$v.log" 2>&1 || { tail -30 "$SAL/logs/cuenta_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la cuenta de medición en $v falló"; }
  log "[$v] aplicando las migraciones de su versión"
  : > "$SAL/logs/migraciones_$v.log"
  local ok=0 mal=0
  for f in $(ls "$d"/src/main/resources/db/migration/V*.sql | sort -V); do
    if psql_db "$v" < "$f" >> "$SAL/logs/migraciones_$v.log" 2>&1; then ok=$((ok+1)); else mal=$((mal+1)); echo "ERROR en $(basename "$f")" >> "$SAL/logs/migraciones_$v.log"; fi
  done
  log "[$v] migraciones: $ok aplicadas, $mal con error (ver logs/migraciones_$v.log)"
  psql_db "$v" -c "ANALYZE" > /dev/null
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'citas='||(SELECT count(*) FROM citas)||' consultas='||(SELECT count(*) FROM consulta)||' mascotas='||(SELECT count(*) FROM mascota)" | tee -a "$SAL/logs/orquestador.log"
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'indices en citas/consulta: '||count(*) FROM pg_indexes WHERE tablename IN ('citas','consulta','prescripciones','purchases','historia_clinica')" | tee -a "$SAL/logs/orquestador.log"
}

cpu_us() { docker exec "sv-$1" cat /sys/fs/cgroup/cpu.stat 2>/dev/null | awk '/^usage_usec/ {print $2}'; }
mem_bytes() { docker exec "sv-$1" cat /sys/fs/cgroup/memory.current 2>/dev/null; }

correr() {
  local v="$1" ronda="$2" iter="$3" d; d="$(dir_de "$v")"
  log "ronda $ronda · $v"
  iniciar_backend "$v" none
  local arranque
  arranque="$(esperar_salud)" || { docker logs "sv-$v" 2>&1 | tail -40 | tee -a "$SAL/logs/orquestador.log"; log "el backend $v no arrancó"; docker rm -f "sv-$v" > /dev/null 2>&1; return 1; }
  sleep 3
  local cpu_arr mem_arr cpu0
  cpu_arr="$(cpu_us "$v")"; mem_arr="$(mem_bytes "$v")"; cpu0="$cpu_arr"
  local company_id
  company_id="$(docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT id FROM company WHERE slug='$SLUG'")"
  docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT pg_stat_statements_reset()" > /dev/null
  : > "$SAL/muestras_mem.txt"
  ( while true; do mem_bytes "$v" >> "$SAL/muestras_mem.txt"; sleep 1; done ) &
  local muestreador=$!
  $PIN_K6 k6 run --quiet \
    -e BASE_URL="http://127.0.0.1:$PUERTO" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e COMPANY_ID="$company_id" \
    -e VUS="$VUS" -e ITER="$iter" -e PAUSA_S="$PAUSA_S" -e SALIDA="$SAL/k6_${v}_${ronda}.json" \
    "$RAIZ/medicion/escenario_p1_p4.js" > "$SAL/logs/k6_${v}_${ronda}.log" 2>&1
  kill "$muestreador" 2> /dev/null; wait "$muestreador" 2> /dev/null
  local cpu1; cpu1="$(cpu_us "$v")"; cpu1="${cpu1:-0}"
  local mem_pico; mem_pico="$(sort -n "$SAL/muestras_mem.txt" | tail -1)"; mem_pico="${mem_pico:-0}"
  cpu_arr="${cpu_arr:-0}"; cpu0="${cpu0:-0}"; mem_arr="${mem_arr:-0}"
  local sql
  sql="$(docker exec sv-pg psql -U postgres -d "$v" -qtAc "SELECT coalesce(sum(calls),0)||'|'||coalesce(round(sum(total_exec_time)::numeric/1000,3),0) FROM pg_stat_statements WHERE dbid=(SELECT oid FROM pg_database WHERE datname='$v')")"
  detener_backend "$v" "$ronda"
  if [ -f "$SAL/k6_${v}_${ronda}.json" ]; then
    jq -n --arg version "$v" --argjson ronda "$ronda" --arg commit "$(git -C "$d" rev-parse --short HEAD)" \
      --argjson arranque "$arranque" --argjson cpu_arr "$cpu_arr" --argjson cpu0 "$cpu0" --argjson cpu1 "$cpu1" \
      --argjson mem_arr "$mem_arr" --argjson mem_pico "$mem_pico" --arg sql "$sql" --slurpfile k6 "$SAL/k6_${v}_${ronda}.json" \
      '{version:$version, ronda:$ronda, commit:$commit, arranque_s:$arranque, cpu_arranque_s:($cpu_arr/1000000),
        mem_tras_arranque_mb:($mem_arr/1048576), cpu_trabajo_s:(($cpu1-$cpu0)/1000000), mem_pico_mb:($mem_pico/1048576),
        sql_sentencias:(($sql|split("|")[0])|tonumber), sql_tiempo_s:(($sql|split("|")[1])|tonumber), k6:$k6[0]}' \
      > "$SAL/corridas/${v}_${ronda}.json" || log "no se pudo armar el resultado de $v ronda $ronda"
  else
    log "k6 no produjo resultados para $v ronda $ronda (ver logs/k6_${v}_${ronda}.log)"
  fi
  sleep "$REPOSO_S"
}

registrar_entorno
construir_imagenes
levantar_postgres
preparar_base antes
preparar_base despues

log "calentamiento (se descarta)"
correr antes 0 3
correr despues 0 3

for ronda in $(seq 1 "$RONDAS"); do
  if [ $((ronda % 2)) -eq 1 ]; then orden="antes despues"; else orden="despues antes"; fi
  for v in $orden; do correr "$v" "$ronda" "$ITER"; done
done

python3 "$RAIZ/medicion/resumir.py" "$SAL" > "$SAL/logs/resumen.log" 2>&1 || log "no se pudo generar el resumen"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && [ -f "$SAL/RESUMEN.md" ] && cat "$SAL/RESUMEN.md" >> "$GITHUB_STEP_SUMMARY"
log "terminado"
