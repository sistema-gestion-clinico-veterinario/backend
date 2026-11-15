#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
SAL="$RAIZ/resultados"
mkdir -p "$SAL/corridas" "$SAL/logs"

FASE="$(tr -d '[:space:]' < "$RAIZ/medicion/render/FASE")"
SLUG="clinica-medicion"
USUARIO="medicion"
CLAVE="${MEDICION_CLAVE:-}"
CUENTAS=3
VUS="${VUS:-1}"
ITER="${ITER:-4}"
PAUSA_S="${PAUSA_S:-1}"
RONDAS="${RONDAS:-4}"
REPOSO_S="${REPOSO_S:-60}"
CITAS_GRANDE="${CITAS_GRANDE:-6000}"
PROP_GRANDE="${PROP_GRANDE:-400}"
XFF_RESTO="${XFF_RESTO:-}"
NIVELES_CARGA="${NIVELES_CARGA:-1,10,25,50,100}"
ETAPA_CARGA="${ETAPA_CARGA:-180s}"
PAUSA_CARGA="${PAUSA_CARGA:-3}"
TIMEOUT_HTTP="${TIMEOUT_HTTP:-180s}"
export PGOPTIONS="-c search_path=public,extensions"

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$SAL/logs/orquestador.log"; }
fallo() { log "FALLO: $*"; exit 1; }

url_de() { if [ "$1" = "antes" ]; then echo "${URL_ANTES%/}"; else echo "${URL_DESPUES%/}"; fi; }
db_de() { if [ "$1" = "antes" ]; then echo "$DB_ANTES"; else echo "$DB_DESPUES"; fi; }
dir_de() { if [ "$1" = "antes" ]; then echo "$RAIZ/.antes"; else echo "$RAIZ/.despues"; fi; }
psql_db() { local v="$1"; shift; psql "$(db_de "$v")" -v ON_ERROR_STOP=1 -q "$@"; }

revisar_secretos() {
  local faltan=""
  for nombre in URL_ANTES URL_DESPUES DB_ANTES DB_DESPUES MEDICION_CLAVE; do
    [ -n "${!nombre:-}" ] || faltan="$faltan $nombre"
  done
  [ -z "$faltan" ] || fallo "faltan secretos de GitHub:$faltan"
}

esperar_salud() {
  local v="$1" t0 codigo
  t0="$(date +%s)"
  for _ in $(seq 1 90); do
    codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time 60 "$(url_de "$v")/api/v1/actuator/health" || true)"
    if [ "$codigo" = "200" ]; then
      echo $(( $(date +%s) - t0 ))
      return 0
    fi
    sleep 10
  done
  return 1
}

iniciar_sesion_curl() {
  local v="$1" jar="$2"
  curl -s -o /dev/null -w '%{http_code}' --max-time 120 -c "$jar" \
    -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
    -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}1\",\"password\":\"$CLAVE\"}" \
    "$(url_de "$v")/api/v1/auth/login"
}

sondeo_limite() {
  local v="$1" jar fijos variados i
  jar="$(mktemp)"
  echo "  inicio de sesión: $(iniciar_sesion_curl "$v" "$jar")"
  fijos="$(for _ in $(seq 1 70); do
    curl -s -o /dev/null -w '%{http_code}\n' --max-time 120 -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
      -H "X-Forwarded-For: 10.200.0.1$XFF_RESTO" "$(url_de "$v")/api/v1/me/navigation"
  done | sort | uniq -c | tr '\n' ';')"
  echo "  70 peticiones con el mismo IP de cliente: $fijos"
  variados="$(for i in $(seq 1 70); do
    curl -s -o /dev/null -w '%{http_code}\n' --max-time 120 -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
      -H "X-Forwarded-For: 10.201.$i.1$XFF_RESTO" "$(url_de "$v")/api/v1/me/navigation"
  done | sort | uniq -c | tr '\n' ';')"
  echo "  70 peticiones con un IP de cliente distinto cada una: $variados"
  rm -f "$jar"
}

fase_verificar() {
  for v in antes despues; do
    log "[$v] esperando respuesta del servicio (puede tardar varios minutos si estaba apagado)"
    local seg
    seg="$(esperar_salud "$v")" || fallo "el servicio $v no respondió en 15 minutos; revisa el registro de Render"
    log "[$v] respondió a los $seg s"
    local roles
    roles="$(psql_db "$v" -tAc "SELECT count(*) FROM roles" 2>&1)" || fallo "no se pudo leer la base de $v: $roles"
    log "[$v] roles en la base: $roles (si es 0, el servicio aún no inicializó los datos)"
    local sembrada
    sembrada="$(psql_db "$v" -tAc "SELECT count(*) FROM company WHERE slug='$SLUG'")"
    if [ "$sembrada" = "1" ]; then
      log "[$v] datos de medición presentes: $(psql_db "$v" -tAc "SELECT 'citas='||(SELECT count(*) FROM citas)||' mascotas='||(SELECT count(*) FROM mascota)")"
      log "[$v] sondeo del límite de peticiones por IP:"
      sondeo_limite "$v" | tee -a "$SAL/logs/orquestador.log"
    else
      log "[$v] todavía sin datos de medición (siguiente fase: sembrar)"
    fi
  done
}

fase_sembrar() {
  for v in antes despues; do
    local d; d="$(dir_de "$v")"
    log "[$v] comprobando la base"
    local roles sembrada
    roles="$(psql_db "$v" -tAc "SELECT count(*) FROM roles" 2>&1)" || fallo "no se pudo leer la base de $v: $roles"
    [ "${roles:-0}" -gt 0 ] || fallo "la base de $v no tiene roles: el servicio no ha inicializado los datos (revisa JPA_DDL_AUTO=update y el registro)"
    sembrada="$(psql_db "$v" -tAc "SELECT count(*) FROM company WHERE slug='$SLUG'")"
    if [ "$sembrada" != "0" ]; then
      log "[$v] ya estaba sembrada; se omite"
      continue
    fi
    log "[$v] sembrando datos ($CITAS_GRANDE citas en la clínica grande)"
    psql_db "$v" -c "CREATE EXTENSION IF NOT EXISTS pgcrypto" > "$SAL/logs/siembra_$v.log" 2>&1 || fallo "no se pudo activar pgcrypto en $v"
    psql_db "$v" -v citas_grande="$CITAS_GRANDE" -v propietarios_grande="$PROP_GRANDE" < "$RAIZ/medicion/sembrar.sql" >> "$SAL/logs/siembra_$v.log" 2>&1 \
      || { tail -20 "$SAL/logs/siembra_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la siembra de $v falló"; }
    : > "$SAL/logs/cuenta_$v.log"
    for n in $(seq 1 "$CUENTAS"); do
      psql_db "$v" -v measurement_company_slug="$SLUG" -v measurement_email="medicion$n@softvet.test" \
        -v measurement_username="$USUARIO$n" -v measurement_password="$CLAVE" < "$RAIZ/medicion/provisionar_cuenta.sql" \
        >> "$SAL/logs/cuenta_$v.log" 2>&1 || { tail -20 "$SAL/logs/cuenta_$v.log" | tee -a "$SAL/logs/orquestador.log"; fallo "la cuenta $n de $v falló"; }
    done
    for f in "$d"/src/main/resources/db/historico/V53__*.sql "$d"/src/main/resources/db/historico/V76__*.sql; do
      tabla="security_rate_limits"; case "$f" in *V76__*) tabla="account_lockouts";; esac
      if [ "$(psql_db "$v" -tAc "SELECT to_regclass('public.$tabla') IS NOT NULL")" = "f" ]; then
        psql_db "$v" < "$f" >> "$SAL/logs/siembra_$v.log" 2>&1 || fallo "la tabla $tabla de $v falló"
      fi
    done
    log "[$v] aplicando las migraciones de su versión"
    : > "$SAL/logs/migraciones_$v.log"
    local ok=0 mal=0
    for f in $(ls "$d"/src/main/resources/db/migration/V*.sql | sort -V); do
      if psql_db "$v" < "$f" >> "$SAL/logs/migraciones_$v.log" 2>&1; then ok=$((ok+1)); else mal=$((mal+1)); echo "ERROR en $(basename "$f")" >> "$SAL/logs/migraciones_$v.log"; fi
    done
    log "[$v] migraciones: $ok aplicadas, $mal con error (ver logs/migraciones_$v.log)"
    psql_db "$v" -c "ANALYZE" > /dev/null
    log "[$v] $(psql_db "$v" -tAc "SELECT 'citas='||(SELECT count(*) FROM citas)||' consultas='||(SELECT count(*) FROM consulta)||' mascotas='||(SELECT count(*) FROM mascota)")"
  done
}

correr() {
  local v="$1" ronda="$2" iter="$3" company_id
  company_id="$(psql_db "$v" -tAc "SELECT id FROM company WHERE slug='$SLUG'")"
  log "ronda $ronda · $v"
  k6 run --quiet \
    -e BASE_URL="$(url_de "$v")" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e COMPANY_ID="$company_id" \
    -e VUS="$VUS" -e ITER="$iter" -e PAUSA_S="$PAUSA_S" -e XFF_RESTO="$XFF_RESTO" -e TIMEOUT="$TIMEOUT_HTTP" \
    -e SALIDA="$SAL/k6_${v}_${ronda}.json" \
    "$RAIZ/medicion/escenario_p1_p4.js" > "$SAL/logs/k6_${v}_${ronda}.log" 2>&1
  if [ -f "$SAL/k6_${v}_${ronda}.json" ]; then
    jq -n --arg version "$v" --argjson ronda "$ronda" --arg commit "$(git -C "$(dir_de "$v")" rev-parse --short HEAD)" \
      --slurpfile k6 "$SAL/k6_${v}_${ronda}.json" '{version:$version, ronda:$ronda, commit:$commit, k6:$k6[0]}' \
      > "$SAL/corridas/${v}_${ronda}.json"
  else
    log "k6 no produjo resultados para $v ronda $ronda (ver logs/k6_${v}_${ronda}.log)"
  fi
}

verificar_calentamiento() {
  local v="$1" f="$SAL/k6_${v}_0.json" malas total
  [ -f "$f" ] || fallo "k6 no produjo resultados en el calentamiento de $v (ver logs/k6_${v}_0.log)"
  malas="$(jq '.respuestas_no_200' "$f")"; total="$(jq '.peticiones' "$f")"
  log "[$v] calentamiento: $malas respuestas distintas de 200 sobre $total peticiones"
  jq -r '.chequeos_fallidos[]?' "$f" | sed 's/^/    /' | tee -a "$SAL/logs/orquestador.log"
  if [ "$total" -gt 0 ] && [ $((malas * 2)) -gt "$total" ]; then
    fallo "más de la mitad de las respuestas de $v no fueron 200 (logs/k6_${v}_0.log)"
  fi
}

fase_medir() {
  for v in antes despues; do
    seg="$(esperar_salud "$v")" || fallo "el servicio $v no respondió"
    log "[$v] listo ($seg s)"
  done
  log "calentamiento (se descarta)"
  for v in antes despues; do
    correr "$v" 0 2
    verificar_calentamiento "$v"
  done
  for ronda in $(seq 1 "$RONDAS"); do
    if [ $((ronda % 2)) -eq 1 ]; then orden="antes despues"; else orden="despues antes"; fi
    for v in $orden; do
      correr "$v" "$ronda" "$ITER"
      sleep "$REPOSO_S"
    done
  done
  python3 "$RAIZ/medicion/resumir.py" "$SAL" > "$SAL/logs/resumen.log" 2>&1 || log "no se pudo generar el resumen"
  {
    echo
    echo "> CPU y memoria del servidor no se miden desde GitHub: se leen del panel Metrics de Render (ver la guía)."
    echo "> Las latencias incluyen la red entre GitHub y Render."
  } >> "$SAL/RESUMEN.md"
  [ -n "${GITHUB_STEP_SUMMARY:-}" ] && [ -f "$SAL/RESUMEN.md" ] && cat "$SAL/RESUMEN.md" >> "$GITHUB_STEP_SUMMARY"
}

fase_vivo() {
  [ -n "${K6_CLOUD_TOKEN:-}" ] && [ -n "${K6_CLOUD_STACK_ID:-}" ] && [ -n "${K6_CLOUD_PROJECT_ID:-}" ] || fallo "faltan secretos de Grafana (MEDICION_K6_TOKEN, MEDICION_K6_STACK y MEDICION_K6_PROJECT)"
  for v in antes despues; do
    seg="$(esperar_salud "$v")" || fallo "el servicio $v no respondió"
    log "[$v] listo ($seg s)"
  done
  for v in antes despues; do
    local company_id
    company_id="$(psql_db "$v" -tAc "SELECT id FROM company WHERE slug='$SLUG'")"
    log "[$v] enviando la prueba a Grafana Cloud en vivo"
    k6 cloud run --local-execution \
      -e BASE_URL="$(url_de "$v")" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e COMPANY_ID="$company_id" \
      -e VUS="$VUS" -e ITER="$ITER" -e PAUSA_S="$PAUSA_S" -e XFF_RESTO="$XFF_RESTO" -e TIMEOUT="$TIMEOUT_HTTP" \
      -e NOMBRE_PRUEBA="Render en vivo - $v" -e SALIDA="$SAL/k6_vivo_${v}.json" \
      "$RAIZ/medicion/escenario_p1_p4.js" > "$SAL/logs/k6_vivo_${v}.log" 2>&1 \
      || log "k6 terminó con avisos en $v (ver logs/k6_vivo_${v}.log)"
    log "[$v] terminada"
  done
}

fase_carga() {
  local versiones="${1:-antes despues}"
  [ -n "${K6_CLOUD_TOKEN:-}" ] && [ -n "${K6_CLOUD_STACK_ID:-}" ] && [ -n "${K6_CLOUD_PROJECT_ID:-}" ] || fallo "faltan secretos de Grafana"
  for v in $versiones; do
    seg="$(esperar_salud "$v")" || fallo "el servicio $v no respondió"
    log "[$v] listo ($seg s)"
  done
  local pids=()
  for v in $versiones; do
    local company_id
    company_id="$(psql_db "$v" -tAc "SELECT id FROM company WHERE slug='$SLUG'")"
    log "[$v] iniciando la prueba escalonada ($NIVELES_CARGA usuarios virtuales, $ETAPA_CARGA por nivel)"
    k6 cloud run --local-execution \
      -e BASE_URL="$(url_de "$v")" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e COMPANY_ID="$company_id" \
      -e PAUSA_S="$PAUSA_CARGA" -e XFF_RESTO="$XFF_RESTO" -e TIMEOUT="$TIMEOUT_HTTP" -e CUENTAS="$CUENTAS" \
      -e PERFIL=carga -e NIVELES="$NIVELES_CARGA" -e ETAPA="$ETAPA_CARGA" \
      -e NOMBRE_PRUEBA="Render carga escalonada - $v" -e SALIDA="$SAL/k6_carga_${v}.json" \
      "$RAIZ/medicion/escenario_p1_p4.js" > "$SAL/logs/k6_carga_${v}.log" 2>&1 &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do wait "$pid" || log "una prueba terminó con avisos (ver logs/k6_carga_*.log)"; done
  log "pruebas terminadas"
  for v in $versiones; do
    [ -f "$SAL/k6_carga_${v}.json" ] && log "[$v] $(jq -c '{iteraciones_completas, peticiones, respuestas_no_200, vus_maximos, p95_ms: .latencia_ms.p95}' "$SAL/k6_carga_${v}.json")"
  done
}

{
  echo "fecha: $(date -u +%FT%TZ)"
  echo "fase: $FASE"
  echo "k6: $(k6 version 2>/dev/null | head -1)"
  echo "psql: $(psql --version)"
  echo "usuarios virtuales: $VUS, iteraciones por ronda: $ITER, rondas: $RONDAS, reposo: $REPOSO_S s, citas de la clínica grande: $CITAS_GRANDE"
  [ -d "$RAIZ/.antes" ] && echo "antes:   $(git -C "$RAIZ/.antes" rev-parse HEAD)"
  [ -d "$RAIZ/.despues" ] && echo "despues: $(git -C "$RAIZ/.despues" rev-parse HEAD)"
} > "$SAL/entorno.txt"
cat "$SAL/entorno.txt" | tee -a "$SAL/logs/orquestador.log"

trap 'python3 "$RAIZ/medicion/render/redactar.py" "$SAL"' EXIT
revisar_secretos
case "$FASE" in
  verificar) fase_verificar ;;
  sembrar) fase_sembrar ;;
  medir) fase_medir ;;
  vivo) fase_vivo ;;
  carga) fase_carga "antes despues" ;;
  carga-antes) fase_carga antes ;;
  carga-despues) fase_carga despues ;;
  *) fallo "fase desconocida en medicion/render/FASE: '$FASE' (usa verificar, sembrar, medir, vivo, carga, carga-antes o carga-despues)" ;;
esac
log "terminado ($FASE)"
