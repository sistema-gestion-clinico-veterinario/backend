#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$RAIZ"
SAL="${RESULTADOS_DIR:-$RAIZ/resultados}"
mkdir -p "$SAL/logs" "$SAL/niveles"
if [ "${LIMPIAR_PREVIO:-0}" = 1 ]; then
  docker rm -f sv-pg sv-correo sv-antes sv-despues > /dev/null 2>&1
  docker network rm sv-net > /dev/null 2>&1
fi

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
ENV_EXTRA_BACKEND="${ENV_EXTRA_BACKEND:-}"
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
LAB_DIR="$RAIZ/medicion/laboratorio"
. "$LAB_DIR/imagenes.sh"
entrada_evento() { [ -n "${GITHUB_EVENT_PATH:-}" ] && [ -f "$GITHUB_EVENT_PATH" ] && jq -r --arg k "$1" '.inputs[$k] // empty' "$GITHUB_EVENT_PATH" 2> /dev/null; true; }
PERFIL_LAB="${PERFIL_LAB:-$(entrada_evento perfil)}"
PERFIL_LAB="${PERFIL_LAB:-CAPACIDAD-2G}"
UMBRAL_GENERADOR_CPU_PCT="${UMBRAL_GENERADOR_CPU_PCT:-$(entrada_evento umbral_generador_cpu_pct)}"
UMBRAL_GENERADOR_CPU_PCT="${UMBRAL_GENERADOR_CPU_PCT:-80}"
HEAP_EN_VIVO="${HEAP_EN_VIVO:-$(entrada_evento heap_en_vivo)}"
HEAP_EN_VIVO="${HEAP_EN_VIVO:-1}"
HEAP_PERIODO_S="${HEAP_PERIODO_S:-5}"
GENERADOR_PERIODO_S="${GENERADOR_PERIODO_S:-2}"
export UMBRAL_GENERADOR_CPU_PCT
CFG_NIVEL=D
CFG_CONTROL=E
CPUS_PREP="$CPUS"
MEM_PREP="$MEMORIA"
PERFIL_EFECTIVO="$PERFIL_LAB"
PIN_MODO="${PIN_MODO:-$(entrada_evento pin_modo)}"
PIN_MODO="${PIN_MODO:-fijo}"
SEMILLA="${SEMILLA:-}"
PESOS_ACCIONES="${PESOS_ACCIONES:-}"
ABORTAR_SI_CAE="${ABORTAR_SI_CAE:-0}"
NMT="${NMT:-0}"
NMT_OPCIONES=""
[ "$NMT" = 1 ] && NMT_OPCIONES="-XX:NativeMemoryTracking=summary -XX:+UnlockDiagnosticVMOptions -XX:+PrintNMTStatistics"
MUESTREO_PG="${MUESTREO_PG:-0}"
VOLCADO_HILOS_S="${VOLCADO_HILOS_S:-}"
LOG_HIKARI="${LOG_HIKARI:-}"
GRACIA_CAIDA_S="${GRACIA_CAIDA_S:-20}"

BASE_JVM="-XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m"
OBSERVACION_JVM="-Xlog:gc,gc+metaspace:file=/tmp/jvm_gc.log:uptime,tags -Xlog:class+load:file=/tmp/jvm_clases.log:uptime,tags -Dspring.mail.properties.mail.smtp.auth=false -Dspring.mail.properties.mail.smtp.starttls.enable=false -Dspring.mail.properties.mail.smtp.starttls.required=false"
flags_de() {
  local f="$1/Dockerfile"
  [ -f "$f" ] || return 1
  tr -d '\r' < "$f" | sed -n 's/^ENV JAVA_OPTS="\(.*\)"[[:space:]]*$/\1/p' | head -1
}
opciones_jvm() {
  case "$1" in
    A) JAVA_OPTS_LAB="-Xmx192m $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
    B) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m" ;;
    C) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m" ;;
    D) JAVA_OPTS_LAB="-Xmx$XMX -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    E) JAVA_OPTS_LAB="-Xmx1g $BASE_JVM -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    F) JAVA_OPTS_LAB="-Xms$XMX -Xmx$XMX -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    G) JAVA_OPTS_LAB="-Xms${XMX_G:-1280m} -Xmx${XMX_G:-1280m} -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=96m -XX:MaxDirectMemorySize=64m $OBSERVACION_JVM" ;;
    P) JAVA_OPTS_LAB="$(flags_de "$DESPUES_DIR") $OBSERVACION_JVM${NMT_OPCIONES:+ $NMT_OPCIONES}"
       case "$JAVA_OPTS_LAB" in " "*) echo "no se pudo leer ENV JAVA_OPTS de $DESPUES_DIR/Dockerfile"; exit 1 ;; esac ;;
    H) JAVA_OPTS_LAB="$(flags_de "$DESPUES_DIR" | sed "s/-Xmx[0-9]*[mMgG]/-Xmx$XMX/") $OBSERVACION_JVM${NMT_OPCIONES:+ $NMT_OPCIONES}"
       case " $JAVA_OPTS_LAB " in *" -Xmx$XMX "*) ;; *) echo "no se pudo fijar -Xmx$XMX en los flags de producción de $DESPUES_DIR/Dockerfile"; exit 1 ;; esac ;;
    *) echo "configuración desconocida: '$1' (usa A, B, C, D, E, F, G, P o H)"; exit 1 ;;
  esac
}
opciones_jvm "$CONFIG"

definir_fijado() {
  NUCLEOS="${NPROC_FORZADO:-$(nproc)}"
  if [ "$NUCLEOS" -ge 4 ]; then
    PIN_PG="--cpuset-cpus 2"
    if awk -v c="$CPUS" 'BEGIN { exit !(c >= 2) }'; then PIN_BACK="--cpuset-cpus 1,3"; else PIN_BACK="--cpuset-cpus 1"; fi
    PIN_K6="taskset -c $K6_NUCLEOS"
  else
    PIN_PG=""
    PIN_BACK=""
    PIN_K6=""
  fi
  PIN_MON=""
  if [ "$NUCLEOS" -ge 4 ] && [ "$PIN_BACK" = "--cpuset-cpus 1" ]; then
    case ",$K6_NUCLEOS," in *,3,*) ;; *) PIN_MON="taskset -c 3" ;; esac
  fi
  AFINIDAD_APLICADA=no
  AFINIDAD_MOTIVO=""
  if [ "$PIN_MODO" = topologia ]; then
    local plan
    plan="$(python3 "$LAB_DIR/afinidad.py" --cpus "$CPUS" ${SYS_CPU_DIR:+--sys "$SYS_CPU_DIR"} --json "$SAL/afinidad_plan.json")"
    eval "$plan"
    [ "$AFINIDAD_APLICADA" = si ] && PIN_K6="taskset -c $K6_NUCLEOS"
  fi
}
definir_fijado

log() { echo "[$(date +%H:%M:%S)] $*" | tee -a "$SAL/logs/orquestador.log"; }
fallo() { log "FALLO: $*"; exit 1; }
a_mb() { case "$1" in *g) echo $(( ${1%g} * 1024 )) ;; *m) echo "${1%m}" ;; *) echo "$1" ;; esac; }
aplicar_perfil() {
  case "$PERFIL_LAB" in
    CAPACIDAD-2G)
      if [ "$MEMORIA" != 2g ] || [ "$CPUS" != 1 ] || [ "$XMX" != 1g ]; then PERFIL_EFECTIVO="CAPACIDAD-2G con parámetros distintos del perfil base (CPU $CPUS, memoria $MEMORIA, heap $XMX)"; fi ;;
    PRODUCCION-512M)
      MEMORIA=512m; XMX=192m; CFG_NIVEL=P; CFG_CONTROL=P; CPUS_PREP=1; MEM_PREP=2g
      PERFIL_EFECTIVO="PRODUCCION-512M"
      [ "$CPUS" = 1 ] || PERFIL_EFECTIVO="PRODUCCION-512M (CPU $CPUS, la de Render es una fracción no verificada)"
      [ -n "$(flags_de "$DESPUES_DIR")" ] || fallo "el perfil PRODUCCION-512M necesita ENV JAVA_OPTS en $DESPUES_DIR/Dockerfile"
      [ -z "${VARIANTES:-}" ] || fallo "el perfil PRODUCCION-512M no se combina con VARIANTES: cada variante define su propia JVM"
      [ "${MODO_SEGURIDAD:-0}" = 0 ] && [ -z "${ESCENARIO_VISUAL:-}" ] && [ "${MODO_CORTE:-0}" = 0 ] \
        || fallo "el perfil PRODUCCION-512M solo aplica a los niveles de carga, no a las pruebas de seguridad, visuales ni de corte" ;;
    EXP-HEAP-*M)
      XMX="${PERFIL_LAB#EXP-HEAP-}"
      XMX="${XMX%M}"
      case "$XMX" in ''|*[!0-9]*) fallo "perfil experimental inválido: '$PERFIL_LAB' (usa EXP-HEAP-<MB>M, por ejemplo EXP-HEAP-256M)" ;; esac
      XMX="${XMX}m"; MEMORIA=512m; CFG_NIVEL=H; CFG_CONTROL=H; CPUS_PREP=1; MEM_PREP=2g
      PERFIL_EFECTIVO="$PERFIL_LAB (experimental: -Xmx$XMX en un contenedor de 512m; el resto de las opciones es el de producción)"
      [ -n "$(flags_de "$DESPUES_DIR")" ] || fallo "el perfil $PERFIL_LAB necesita ENV JAVA_OPTS en $DESPUES_DIR/Dockerfile"
      [ -z "${VARIANTES:-}" ] || fallo "el perfil $PERFIL_LAB no se combina con VARIANTES"
      [ "${MODO_SEGURIDAD:-0}" = 0 ] && [ -z "${ESCENARIO_VISUAL:-}" ] && [ "${MODO_CORTE:-0}" = 0 ] \
        || fallo "el perfil $PERFIL_LAB solo aplica a los niveles de carga" ;;
    *) fallo "perfil desconocido: '$PERFIL_LAB' (usa CAPACIDAD-2G, PRODUCCION-512M o EXP-HEAP-<MB>M)" ;;
  esac
  case "$UMBRAL_GENERADOR_CPU_PCT" in ''|*[!0-9.]*) fallo "umbral del generador no numérico: '$UMBRAL_GENERADOR_CPU_PCT'" ;; esac
  case "$HEAP_EN_VIVO" in 0|1) ;; *) fallo "HEAP_EN_VIVO debe ser 0 o 1" ;; esac
}
verificar_k6() {
  local version; version="$(k6 version | head -1)"
  case "$version" in
    "k6 v$K6_VERSION "*|"k6.exe v$K6_VERSION "*) ;;
    *) [ "${K6_VERSION_LIBRE:-0}" = 1 ] || fallo "k6 no es la versión fijada $K6_VERSION: $version (K6_VERSION_LIBRE=1 lo permite)" ;;
  esac
}
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
    echo "perfil del laboratorio: $PERFIL_EFECTIVO (memoria $MEMORIA, CPU $CPUS, heap máximo $XMX; configuración de la JVM en los niveles: $CFG_NIVEL, en el control: $CFG_CONTROL)"
    echo "umbral del generador: $UMBRAL_GENERADOR_CPU_PCT % de la CPU de k6 (criterio experimental, configurable)"
    echo "modo de afinidad: $PIN_MODO (aplicado: $AFINIDAD_APLICADA${AFINIDAD_MOTIVO:+; motivo: $AFINIDAD_MOTIVO}); k6 en CPU $K6_NUCLEOS, backend '$PIN_BACK', PostgreSQL '$PIN_PG', muestreadores '$PIN_MON'"
    echo "mezcla de operaciones: semilla ${SEMILLA:-no fijada (aleatoria)}; pesos ${PESOS_ACCIONES:-22,18,15,2,13,12,10,8} (panel, agenda, historia, reportes, recepción, atención, cobro, cancelar)"
    echo "citas de la clínica grande: ${CITAS_GRANDE:-20000}; propietarios: ${PROPIETARIOS_GRANDE:-1200}"
    echo "pool de conexiones: $POOL; muestreo de PostgreSQL: $([ "$MUESTREO_PG" = 1 ] && echo sí || echo no); volcados de hilos en los segundos: ${VOLCADO_HILOS_S:-ninguno}; registro de Hikari en DEBUG: $([ -n "$LOG_HIKARI" ] && echo sí || echo no); NMT: $([ "$NMT" = 1 ] && echo "sí (añade sobrecosto de memoria nativa)" || echo no)"
    echo "caída del backend: $([ "$ABORTAR_SI_CAE" = 1 ] && echo "k6 se detiene $GRACIA_CAIDA_S s después de que el contenedor se detenga" || echo "k6 continúa hasta el final")"
    echo "heap en vivo: $([ "$HEAP_EN_VIVO" = 1 ] && echo "sí, cada $HEAP_PERIODO_S s (contadores hsperfdata y cgroup)" || echo no)"
    echo "runner: ${ImageOS:-desconocido} ${ImageVersion:-desconocida}"
    echo "kernel: $(uname -r)"
    echo "configuraciones de la JVM:"
    for c in C D E F G; do opciones_jvm "$c"; echo "  $c -> $JAVA_OPTS_LAB"; done
    if [ -n "$(flags_de "$DESPUES_DIR")" ]; then
      opciones_jvm P; echo "  P -> $JAVA_OPTS_LAB"
      echo "  (P = ENV JAVA_OPTS del Dockerfile de producción de DESPUÉS, $(git -C "$DESPUES_DIR" rev-parse --short HEAD 2>/dev/null || echo sin git), más las opciones de observación del arnés: registro de GC y propiedades del correo de pruebas)"
      [ ! -f "$ANTES_DIR/Dockerfile" ] || [ "$(flags_de "$ANTES_DIR")" = "$(flags_de "$DESPUES_DIR")" ] || echo "  AVISO: el Dockerfile de ANTES tiene JAVA_OPTS distintos al de DESPUÉS"
    else
      echo "  P -> no disponible (DESPUÉS no tiene Dockerfile con ENV JAVA_OPTS)"
    fi
    if [ "$CFG_NIVEL" = D ] && [ "$CFG_CONTROL" = E ]; then
      echo "usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control"
    else
      echo "usadas en esta corrida: $CFG_NIVEL para los niveles y $CFG_CONTROL para el control (perfil $PERFIL_EFECTIVO)"
    fi
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

fijar_dockerfile() {
  awk -v build="$IMG_MAVEN" -v ejecucion="$IMG_JRE" '
    /^FROM maven:3\.9\.6-eclipse-temurin-21-alpine AS build[[:space:]\r]*$/ { print "FROM " build " AS build"; n++; next }
    /^FROM eclipse-temurin:21-jre-alpine[[:space:]\r]*$/ { print "FROM " ejecucion; n++; next }
    { print }
    END { if (n != 2) exit 3 }' "$DESPUES_DIR/Dockerfile.measurement" > "$SAL/Dockerfile.fijado"
  if [ $? -ne 0 ]; then
    [ "${BASES_ESTRICTAS:-0}" = 1 ] && fallo "Dockerfile.measurement no tiene las dos líneas FROM esperadas: no se pueden fijar las bases por digest"
    cp "$DESPUES_DIR/Dockerfile.measurement" "$SAL/Dockerfile.fijado"
    BASES_FIJADAS=no
    log "AVISO: Dockerfile.measurement no tiene las dos líneas FROM esperadas; las bases NO quedan fijadas por digest en esta corrida"
  fi
}

descargar_imagen() {
  local ref="$1" intento
  for intento in 1 2 3; do
    docker pull -q "$ref" > /dev/null 2>&1 && return 0
    sleep $((intento * 10))
  done
  fallo "no se pudo descargar $ref"
}

registrar_imagenes() {
  local ref digest repo_digests
  {
    echo "imágenes fijadas por digest (referencia; id local):"
    for ref in "$IMG_POSTGRES" "$IMG_PYTHON" "$IMG_MAVEN" "$IMG_JRE"; do
      descargar_imagen "$ref"
      digest="${ref#*@}"
      repo_digests="$(docker image inspect "$ref" --format '{{join .RepoDigests ","}}')"
      case "$repo_digests" in
        *"$digest"*) echo "  imagen: $ref (id $(docker image inspect "$ref" --format '{{.Id}}'); digest verificado por Docker al descargar)" ;;
        *) echo "  imagen: $ref NO COINCIDE con los digests locales: $repo_digests"; fallo "el digest de $ref no coincide" ;;
      esac
    done
    echo "  bases del Dockerfile.measurement fijadas por digest: ${BASES_FIJADAS:-sí}"
    echo "  Dockerfile.measurement usado: $SAL/Dockerfile.fijado (sha256 $(sha256sum "$SAL/Dockerfile.fijado" | cut -d' ' -f1))"
    echo "  k6 fijado: $K6_VERSION"
  } >> "$SAL/entorno.txt"
}

registrar_herramientas_java() {
  docker run --rm --entrypoint sh "$IMG_JRE" -c 'java -version 2>&1; for t in java jcmd jstat jfr jps; do if command -v $t > /dev/null 2>&1; then echo "$t: sí"; else echo "$t: no"; fi; done' > "$SAL/herramientas_java_jre.txt" 2>&1 || true
  {
    echo "imagen de ejecución (JRE): $IMG_JRE"
    sed 's/^/  /' "$SAL/herramientas_java_jre.txt"
    echo "  el heap en vivo se lee de los contadores de la JVM (/tmp/hsperfdata_*/1) con cat dentro del contenedor, sin jstat ni jcmd"
  } >> "$SAL/entorno.txt"
}

registrar_topologia() {
  {
    echo "### lscpu -e"
    lscpu -e=CPU,CORE,SOCKET,NODE,ONLINE,MAXMHZ 2>&1 || lscpu -e 2>&1
    echo
    echo "### lscpu (resumen)"
    lscpu | grep -E 'Model name|Thread|Core|Socket|Hypervisor|Virtualization|L1d|L2|L3' 2>&1
    echo
    echo "### afinidad del arnés: $(taskset -cp $$ 2>&1)"
    echo "### cpuset del cgroup del arnés: $(cat /sys/fs/cgroup/cpuset.cpus.effective 2>/dev/null || echo no disponible)"
    echo "### fijado de CPU solicitado: PG='$PIN_PG' backend='$PIN_BACK' k6='$PIN_K6' muestreadores='$PIN_MON'"
  } > "$SAL/topologia_lscpu.txt"
  python3 "$LAB_DIR/topologia.py" --pg "${PIN_PG#--cpuset-cpus}" --backend "${PIN_BACK#--cpuset-cpus}" \
    --k6 "$([ -n "$PIN_K6" ] && echo "$K6_NUCLEOS")" --mon "${PIN_MON#taskset -c}" --json "$SAL/topologia.json" > "$SAL/topologia.md" 2> "$SAL/logs/topologia.log" \
    || log "no se pudo generar la topología (ver logs/topologia.log)"
  {
    echo
    echo "### Modo de afinidad: $PIN_MODO"
    if [ "$PIN_MODO" = topologia ]; then
      if [ "$AFINIDAD_APLICADA" = si ]; then
        python3 -c "import json,sys;[print('- ' + n) for n in json.load(open(sys.argv[1], encoding='utf-8'))['notas']]" "$SAL/afinidad_plan.json"
      else
        echo "- no se aplicó: $AFINIDAD_MOTIVO"
      fi
    else
      echo "- fijado anterior (k6 en CPU $K6_NUCLEOS, backend '$PIN_BACK', PostgreSQL '$PIN_PG'), sin considerar qué CPU lógicas son hermanas SMT"
    fi
  } >> "$SAL/topologia.md"
  cat "$SAL/topologia_lscpu.txt" "$SAL/topologia.md" >> "$SAL/logs/orquestador.log" 2> /dev/null
}

registrar_io() {
  local v="$1" etiqueta="$2" fase="$3" par
  {
    for par in "sv-$v:backend" "sv-pg:postgres"; do
      echo "[${par#*:}]"
      echo "io.stat: $(docker exec "${par%%:*}" cat /sys/fs/cgroup/io.stat 2> /dev/null | tr '\n' ';')"
      echo "cpu.stat: $(docker exec "${par%%:*}" cat /sys/fs/cgroup/cpu.stat 2> /dev/null | tr '\n' ';')"
      echo "memory.events: $(docker exec "${par%%:*}" cat /sys/fs/cgroup/memory.events 2> /dev/null | tr '\n' ';')"
      echo "red: $(docker exec "${par%%:*}" cat /proc/net/dev 2> /dev/null | awk '/eth0/ {print "rx_bytes=" $2 ";tx_bytes=" $10}')"
    done
  } > "$SAL/niveles/io_${etiqueta}_${fase}.txt" 2>&1
}

volcar_hilos() {
  local v="$1" etiqueta="$2" inicio="$3" seg marca
  for seg in $(echo "$VOLCADO_HILOS_S" | tr ',' ' '); do
    while [ $(( $(date +%s) - inicio )) -lt "$seg" ]; do sleep 1; done
    marca="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    docker kill -s QUIT "sv-$v" > /dev/null 2>&1 || return 0
    sleep 4
    docker logs --since "$marca" "sv-$v" 2>&1 | sed -n '/^Full thread dump/,$p' | head -c 800000 | gzip > "$SAL/niveles/hilos_${etiqueta}_t${seg}.txt.gz"
  done
}

vigilar_backend() {
  local v="$1" etiqueta="$2" corriendo
  while sleep 2; do
    corriendo="$(docker inspect -f '{{.State.Running}}' "sv-$v" 2> /dev/null)"
    if [ "$corriendo" = false ]; then
      echo "t=$(date -u +%FT%TZ) $(docker inspect -f 'OOMKilled={{.State.OOMKilled}} codigo_salida={{.State.ExitCode}} error={{.State.Error}}' "sv-$v" 2>&1)" > "$SAL/niveles/caida_${etiqueta}.txt"
      log "[$v] el contenedor del backend se detuvo en $etiqueta ($(cat "$SAL/niveles/caida_${etiqueta}.txt")); k6 se detiene en $GRACIA_CAIDA_S s"
      sleep "$GRACIA_CAIDA_S"
      pkill -TERM -x k6 2> /dev/null
      return 0
    fi
  done
}

registrar_afinidad() {
  local v="$1" etiqueta="$2"
  {
    echo "backend sv-$v: $(docker inspect "sv-$v" --format 'cpuset={{.HostConfig.CpusetCpus}} nanocpus={{.HostConfig.NanoCpus}} memoria={{.HostConfig.Memory}} swap={{.HostConfig.MemorySwap}}')"
    echo "backend, cpuset.cpus.effective: $(docker exec "sv-$v" cat /sys/fs/cgroup/cpuset.cpus.effective 2>/dev/null || echo no disponible)"
    echo "backend, cpu.max: $(docker exec "sv-$v" cat /sys/fs/cgroup/cpu.max 2>/dev/null || echo no disponible)"
    echo "backend, memory.max: $(docker exec "sv-$v" cat /sys/fs/cgroup/memory.max 2>/dev/null || echo no disponible)"
    echo "backend, nproc dentro del contenedor: $(docker exec "sv-$v" nproc 2>/dev/null || echo no disponible)"
    echo "postgres sv-pg: $(docker inspect sv-pg --format 'cpuset={{.HostConfig.CpusetCpus}} nanocpus={{.HostConfig.NanoCpus}} memoria={{.HostConfig.Memory}}')"
    echo "postgres, cpuset.cpus.effective: $(docker exec sv-pg cat /sys/fs/cgroup/cpuset.cpus.effective 2>/dev/null || echo no disponible)"
  } > "$SAL/niveles/afinidad_${etiqueta}.txt" 2>&1
}

construir_imagenes() {
  for v in $VERSIONES; do
    local d; d="$(dir_de "$v")"
    log "construyendo la imagen $v desde $(git -C "$d" rev-parse --short HEAD)"
    local intento construida=0
    for intento in 1 2 3 4; do
      if docker build -f "$SAL/Dockerfile.fijado" -t "sv-backend:$v" "$d" > "$SAL/logs/build_$v.log" 2>&1; then construida=1; break; fi
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
  docker run -d --name sv-pg --network sv-net $PIN_PG --memory 2g -e POSTGRES_PASSWORD="$PGPASS" "$IMG_POSTGRES" \
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
  docker run -d --name sv-correo --network sv-net -v "$RAIZ/medicion/escritura/sumidero_smtp.py:/sumidero.py:ro" "$IMG_PYTHON" python -u /sumidero.py > /dev/null \
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
    ${ENV_EXTRA_BACKEND:+-e $ENV_EXTRA_BACKEND} ${LOG_HIKARI:+-e LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG} \
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

detener_backend() { [ "$NMT" = 1 ] && docker stop -t 30 "sv-$1" > /dev/null 2>&1; docker logs "sv-$1" > "$SAL/logs/backend_$2.log" 2>&1; docker rm -f "sv-$1" > /dev/null 2>&1 || true; }

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
  iniciar_backend "$v" none "$CPUS_PREP" "$MEM_PREP" "$PIN_BACK"
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

sonda_caida() {
  local jar="$1" fase="$2" etiqueta="$3" base="http://127.0.0.1:$PUERTO/api/v1" espera="${ESPERA_SONDA_S:-15}" d
  d="$(mktemp -d)"
  (
    codigo="$(curl -s -o /dev/null -D "$d/cab" -w '%{http_code}' --max-time "$espera" -b "$jar" -X POST -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.9" "$base/auth/refresh")"
    cookies="$(grep -i '^set-cookie' "$d/cab" 2> /dev/null | tr -d '\r' | sed -E 's/^[Ss]et-[Cc]ookie: ([^=]+)=[^;]*;.*[Mm]ax-[Aa]ge=([0-9]+).*/\1 (Max-Age=\2)/' | paste -sd, -)"
    echo "$codigo|cookies enviadas por la respuesta: ${cookies:-ninguna}" > "$d/1"
  ) &
  (
    codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time "$espera" -b "$jar" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.9" "$base/me/navigation")"
    echo "$codigo|" > "$d/2"
  ) &
  (
    codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time "$espera" -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.8" -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}1\",\"password\":\"$CLAVE\"}" "$base/auth/login")"
    echo "$codigo|" > "$d/3"
  ) &
  wait
  local n nombre
  for n in 1 2 3; do
    case "$n" in 1) nombre="POST /auth/refresh" ;; 2) nombre="GET /me/navigation (con sesión)" ;; 3) nombre="POST /auth/login" ;; esac
    echo "$fase|$nombre|$(cat "$d/$n" 2> /dev/null || echo '000|')" >> "$SAL/niveles/sonda_caida_${etiqueta}.txt"
  done
  rm -rf "$d"
}

caos_en_segundo_plano() {
  local v="$1" etiqueta="$2" evento="$3" espera="$4"
  local t_inicio t_evento t_bd_listo="" t_backend_ok="" i seq_antes="" seq_despues=""
  secuencia_purchases() { docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT last_value || '|' || (SELECT coalesce(max(id), 0) FROM purchases) FROM purchases_id_seq" 2> /dev/null | tr -d '[:space:]'; }
  t_inicio="$(date +%s.%N)"
  sleep "$espera"
  case "$evento" in bd_*) seq_antes="$(secuencia_purchases)" ;; esac
  local jar=""
  if [ "$evento" = "bd_parada" ]; then
    jar="$(mktemp)"
    : > "$SAL/niveles/sonda_caida_${etiqueta}.txt"
    curl -s -o /dev/null -c "$jar" -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: 10.250.0.9" \
      -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}1\",\"password\":\"$CLAVE\"}" "http://127.0.0.1:$PUERTO/api/v1/auth/login"
  fi
  t_evento="$(date +%s.%N)"
  case "$evento" in
    bd_reinicio) docker restart sv-pg > /dev/null 2>&1 ;;
    bd_kill) docker kill sv-pg > /dev/null 2>&1; docker start sv-pg > /dev/null 2>&1 ;;
    bd_kill_checkpoint) docker exec sv-pg psql -U postgres -qc "CHECKPOINT" > /dev/null 2>&1; docker kill sv-pg > /dev/null 2>&1; docker start sv-pg > /dev/null 2>&1 ;;
    bd_parada)
      docker stop sv-pg > /dev/null 2>&1
      local t_parada restante; t_parada="$(date +%s)"
      if [ "${ESPERA_SONDA_S:-15}" -gt 0 ]; then
        sleep "${SONDA_INICIO_S:-8}"
        sonda_caida "$jar" "con la base caída" "$etiqueta"
      fi
      restante=$(( ${PARADA_BD_S:-30} - ($(date +%s) - t_parada) ))
      [ "$restante" -gt 0 ] && sleep "$restante"
      docker start sv-pg > /dev/null 2>&1 ;;
    backend_kill) docker kill "sv-$v" > /dev/null 2>&1; docker start "sv-$v" > /dev/null 2>&1 ;;
  esac
  case "$evento" in
    bd_*)
      for i in $(seq 1 300); do
        docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 && { t_bd_listo="$(date +%s.%N)"; break; }
        sleep 0.5
      done
      seq_despues="$(secuencia_purchases)" ;;
  esac
  for i in $(seq 1 600); do
    if curl -fsS --max-time 2 "http://127.0.0.1:$PUERTO/api/v1/actuator/health" > /dev/null 2>&1; then t_backend_ok="$(date +%s.%N)"; break; fi
    sleep 0.5
  done
  [ -n "$jar" ] && [ "${ESPERA_SONDA_S:-15}" -gt 0 ] && sonda_caida "$jar" "con la base recuperada" "$etiqueta"
  [ -n "$jar" ] && rm -f "$jar"
  jq -n --arg evento "$evento" --arg inicio "$t_inicio" --arg evento_t "$t_evento" --arg bd "$t_bd_listo" --arg be "$t_backend_ok" --arg sa "$seq_antes" --arg sd "$seq_despues" \
    '{secuencia_antes:$sa, secuencia_despues:$sd, evento:$evento, t_inicio_k6:($inicio|tonumber), t_evento:($evento_t|tonumber), t_bd_listo:(if $bd=="" then null else ($bd|tonumber) end), t_backend_ok:(if $be=="" then null else ($be|tonumber) end)}' \
    > "$SAL/niveles/caos_${etiqueta}.json"
}

correr_nivel_escritura() {
  local v="$1" nivel="$2" rampa="$3" mantener="$4" base_u="$5" base_cuenta="$6" config="${7:-$CFG_NIVEL}" prefijo="${8:-$1}" prep="${9:-}" envx="${10:-}" inicio_iso
  local etiqueta="${prefijo}_${nivel}" etq_caja="$nivel" t0 salud ids codigo cpu0 cpu1 muestreador muestreador_pg muestreador_gen muestreador_jvm muestreador_pgact vigia_hilos vigilante caida correos0 correos1
  [ "$prefijo" != "$v" ] && etq_caja="${prefijo}-${nivel}"
  opciones_jvm "$config"
  ENV_EXTRA_BACKEND="$envx"
  if [ "$RESET_BASE" = 1 ]; then reiniciar_base "$v" "$prep" "$etiqueta"; fi
  docker exec sv-pg psql -U postgres -d "$v" -tAc "SELECT 'plan_cache_mode=' || current_setting('plan_cache_mode') || ' jit=' || current_setting('jit')" > "$SAL/niveles/config_bd_${etiqueta}.txt" 2> /dev/null || true
  log "[$v] nivel $nivel (config $config, $etiqueta): arrancando el backend ($CPUS CPU, $MEMORIA)"
  iniciar_backend "$v" none "$CPUS" "$MEMORIA" "$PIN_BACK"
  esperar_inicializacion "$v"
  registrar_afinidad "$v" "$etiqueta"
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
  $PIN_MON python3 "$LAB_DIR/muestreo_generador.py" "$SAL/niveles/generador_${etiqueta}.csv" --periodo "$GENERADOR_PERIODO_S" &
  muestreador_gen=$!
  muestreador_jvm=""
  if [ "$HEAP_EN_VIVO" = 1 ]; then
    $PIN_MON python3 "$LAB_DIR/muestreo_jvm.py" "$SAL/niveles/jvm_vivo_${etiqueta}.csv" --contenedor "sv-$v" --periodo "$HEAP_PERIODO_S" &
    muestreador_jvm=$!
  fi
  muestreador_pgact=""
  if [ "$MUESTREO_PG" = 1 ]; then
    $PIN_MON python3 "$LAB_DIR/muestreo_pg.py" "$SAL/niveles/pg_actividad_${etiqueta}.csv" --base "$v" --periodo 2 &
    muestreador_pgact=$!
  fi
  registrar_io "$v" "$etiqueta" antes
  vigilante=""
  if [ "$ABORTAR_SI_CAE" = 1 ]; then
    vigilar_backend "$v" "$etiqueta" &
    vigilante=$!
  fi
  t0="$(date +%s)"
  inicio_iso="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  vigia_hilos=""
  if [ -n "$VOLCADO_HILOS_S" ]; then
    volcar_hilos "$v" "$etiqueta" "$t0" &
    vigia_hilos=$!
  fi
  log "[$v] nivel $nivel: k6 con hasta $nivel usuarios (subida $rampa, sostenido $mantener)"
  local evento_caos="" pid_caos=""
  case "$prefijo" in caos_*) evento_caos="${prefijo#caos_}"; evento_caos="${evento_caos%_antes}"; evento_caos="${evento_caos%_despues}"; evento_caos="${evento_caos%_rapido}"; evento_caos="${evento_caos%_medio}" ;; esac
  if [ -n "$evento_caos" ]; then
    log "[$v] evento de caos programado: $evento_caos a los ${ESPERA_CAOS:-150} s del inicio de la carga"
    caos_en_segundo_plano "$v" "$etiqueta" "$evento_caos" "${ESPERA_CAOS:-150}" &
    pid_caos=$!
  fi
  $PIN_K6 env K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PERIOD=5s K6_WEB_DASHBOARD_EXPORT="$SAL/niveles/panel_k6_${etiqueta}.html" \
    k6 run --quiet --out "csv=$SAL/niveles/k6_${etiqueta}.csv" \
    -e BASE_URL="http://127.0.0.1:$PUERTO" -e SLUG="$SLUG" -e USUARIO="$USUARIO" -e CLAVE="$CLAVE" -e IDS="$ids" \
    -e CUENTA_BASE="$base_cuenta" -e U_BASE="$base_u" -e ITER_POR_VU="$ITER_POR_VU" -e ETIQUETA_NIVEL="$etq_caja" -e NIVEL="$nivel" -e RAMPA="$rampa" -e MANTENER="$mantener" \
    -e PAUSA_S="$PAUSA_S" -e SEMILLA="$SEMILLA" -e PESOS_ACCIONES="$PESOS_ACCIONES" -e TIMEOUT="$TIMEOUT_HTTP" -e SALIDA="$SAL/niveles/k6_${etiqueta}.json" \
    "$RAIZ/medicion/escritura/carga_escrituras.js" > "$SAL/logs/k6_${etiqueta}.log" 2>&1
  codigo=$?
  [ -n "$pid_caos" ] && wait "$pid_caos"
  kill "$muestreador" "$muestreador_pg" $muestreador_gen $muestreador_jvm $muestreador_pgact $vigia_hilos 2> /dev/null; wait "$muestreador" "$muestreador_pg" $muestreador_gen $muestreador_jvm $muestreador_pgact $vigia_hilos 2> /dev/null
  [ -n "$vigilante" ] && { kill "$vigilante" 2> /dev/null; wait "$vigilante" 2> /dev/null; }
  registrar_io "$v" "$etiqueta" despues
  caida=""
  [ -f "$SAL/niveles/caida_${etiqueta}.txt" ] && caida="$(cat "$SAL/niveles/caida_${etiqueta}.txt")"
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
    --arg heap_max "$XMX" --arg perfil "$PERFIL_EFECTIVO" --arg caida "$caida" --argjson cpus_limite "$CPUS" --argjson memoria_limite_mb "$(a_mb "$MEMORIA")" \
    '{version:$version, perfil_lab:$perfil, nivel:$nivel, config_jvm:$config, correos_recibidos:$correos, heap_max:$heap_max, cpus_limite:$cpus_limite, memoria_limite_mb:$memoria_limite_mb, perfil:"carga_escritura", duracion_s:$duracion, cpu_contenedor_s:$cpu_s, oom_killed:($oom=="true"),
      reinicios:$reinicios, sigue_corriendo:($corriendo=="true"), codigo_salida:$salida, salud_final_http:$salud,
      k6_codigo_salida:$k6_codigo, caida_backend:$caida, sql_sentencias:(($sql|split("|")[0])|tonumber), sql_tiempo_s:(($sql|split("|")[1])|tonumber)}' \
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
  if [ "$NMT" = 1 ]; then
    sed -n '/^Native Memory Tracking:/,$p' "$SAL/logs/backend_carga_${etiqueta}.log" > "$SAL/niveles/nmt_${etiqueta}.txt"
    [ -s "$SAL/niveles/nmt_${etiqueta}.txt" ] || log "[$v] no se obtuvo el resumen de memoria nativa de $etiqueta (la JVM no terminó de forma ordenada)"
  fi
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

aplicar_perfil
verificar_k6
registrar_entorno
registrar_topologia
fijar_dockerfile
registrar_imagenes
registrar_herramientas_java
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
if [ -n "${ESCENARIO_VISUAL:-}" ]; then
  for v in $VERSIONES; do preparar_clinica "$v"; done
  mkdir -p "$SAL/visual"
  for v in $VERSIONES; do
    log "[$v] prueba visual ($ESCENARIO_VISUAL)"
    iniciar_backend "$v" none 1 2g "$PIN_BACK"
    esperar_salud > /dev/null || fallo "el backend $v no arrancó"
    esperar_inicializacion "$v"
    export PUERTO SLUG USUARIO CLAVE SAL RAIZ ESCENARIO_VISUAL FRONT_DIST_antes FRONT_DIST_despues
    bash "$RAIZ/medicion/visual/prueba_visual.sh" "$v" || true
    detener_backend "$v" "visual_${ESCENARIO_VISUAL}_$v"
  done
  python3 "$RAIZ/medicion/visual/informe_visual.py" "$SAL" "$ESCENARIO_VISUAL" > "$SAL/logs/informe_visual_$ESCENARIO_VISUAL.log" 2>&1 \
    && cat "$SAL/visual/INFORME_$ESCENARIO_VISUAL.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
    || log "no se pudo generar el informe visual (ver logs/informe_visual_$ESCENARIO_VISUAL.log)"
  log "terminado (prueba visual $ESCENARIO_VISUAL)"
  exit 0
fi
if [ "${MODO_CORTE:-0}" = 1 ]; then
  for v in $VERSIONES; do preparar_clinica "$v"; done
  for v in $VERSIONES; do
    log "[$v] corte de PostgreSQL con peticiones en curso"
    iniciar_backend "$v" none 1 2g "$PIN_BACK"
    esperar_salud > /dev/null || fallo "el backend $v no arrancó"
    esperar_inicializacion "$v"
    export PUERTO SLUG USUARIO CLAVE SAL POOL CICLOS_CORTE TRABAJADORES_CORTE ESPERA_CORTE_S
    bash "$RAIZ/medicion/escritura/prueba_corte.sh" "$v" || true
    cat "$SAL/corte_$v.txt" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    detener_backend "$v" "corte_$v"
  done
  log "terminado (corte con peticiones en curso)"
  exit 0
fi
for v in $VERSIONES; do preparar_clinica "$v"; done
[ "$RESET_BASE" = 1 ] && crear_plantilla despues

if [ "$SIN_CONTROL" != 1 ]; then
  log "control: $CONTROL_USUARIOS usuarios con Metaspace de 128 MB (igual que el Dockerfile) y el servidor de correo de pruebas"
  correr_nivel_escritura despues "$CONTROL_USUARIOS" 30s 150s 0 1 "$CFG_CONTROL" ctrl128
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
  if [ -z "$VARIANTES" ] && [ "$RESET_BASE" != 1 ]; then
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
python3 "$LAB_DIR/informe_laboratorio.py" "$SAL" > "$SAL/logs/informe_laboratorio.log" 2>&1 \
  && cat "$SAL/SALUD_LABORATORIO.md" >> "${GITHUB_STEP_SUMMARY:-/dev/null}" \
  || log "no se pudo generar el informe de salud del laboratorio (ver logs/informe_laboratorio.log)"
log "terminado"
