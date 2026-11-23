#!/bin/bash
set -uo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
LAB="$RAIZ/medicion/laboratorio"
. "$LAB/imagenes.sh"
SAL="${SAL:-$RAIZ/resultados-laboratorio}"
VENTANA_S="${VENTANA_S:-60}"
MUESTRAS="${MUESTRAS:-12}"
PERIODO_S="${PERIODO_S:-5}"
UMBRAL_INTERFERENCIA_PCT="${UMBRAL_INTERFERENCIA_PCT:-1}"
mkdir -p "$SAL"
REPORTE="$SAL/CALIBRACION.md"
: > "$REPORTE"
FALLOS=0
nota() { echo "$*" | tee -a "$REPORTE"; }
error() { nota "- **FALLA:** $*"; FALLOS=$((FALLOS + 1)); }
limpiar() {
  docker rm -f sv-cal > /dev/null 2>&1 || true
  [ -n "${SERVIDOR:-}" ] && kill "$SERVIDOR" 2> /dev/null
  true
}
trap limpiar EXIT

nota "# Calibración de la instrumentación del laboratorio"
nota ""
nota "- fecha: $(date -u +%FT%TZ)"
nota "- runner: ${ImageOS:-desconocido} ${ImageVersion:-desconocida}; kernel $(uname -r)"
nota "- docker: $(docker version --format '{{.Server.Version}}')"
nota "- k6: $(k6 version | head -1) (fijada: $K6_VERSION)"
case "$(k6 version | head -1)" in "k6 v$K6_VERSION "*) ;; *) error "k6 no es la versión fijada $K6_VERSION" ;; esac
nota ""

nota "## 1. Topología y afinidad de CPU"
nota ""
{
  lscpu -e=CPU,CORE,SOCKET,NODE,ONLINE,MAXMHZ 2>&1 || lscpu -e 2>&1
  lscpu | grep -E 'Model name|Thread|Core|Socket|Hypervisor|Virtualization|L1d|L2|L3'
  echo "afinidad del arnés: $(taskset -cp $$ 2>&1)"
} > "$SAL/lscpu.txt"
nota '```'
cat "$SAL/lscpu.txt" | tee -a "$REPORTE"
nota '```'
python3 "$LAB/topologia.py" --pg 2 --backend 1 --k6 0 --mon 3 --json "$SAL/topologia.json" > "$SAL/topologia.md" || error "topologia.py falló"
cat "$SAL/topologia.md" >> "$REPORTE"
nota ""

nota "## 2. Herramientas Java y versión en las imágenes (fijadas por digest)"
nota ""
nota "| Imagen | Versión de Java | jcmd | jstat | jfr | jps |"
nota "|---|---|---|---|---|---|"
for ref in "$IMG_JRE" "$IMG_JDK"; do
  docker pull -q "$ref" > /dev/null 2>&1 || { error "no se pudo descargar $ref"; continue; }
  salida="$(docker run --rm --entrypoint sh "$ref" -c 'java -version 2>&1 | head -1; for t in jcmd jstat jfr jps; do if command -v $t > /dev/null 2>&1; then echo "$t: sí"; else echo "$t: no"; fi; done')"
  version="$(echo "$salida" | head -1)"
  nota "| \`$ref\` | $version | $(echo "$salida" | grep '^jcmd' | cut -d' ' -f2) | $(echo "$salida" | grep '^jstat' | cut -d' ' -f2) | $(echo "$salida" | grep '^jfr' | cut -d' ' -f2) | $(echo "$salida" | grep '^jps' | cut -d' ' -f2) |"
  case "$version" in *'"21.'*) ;; *) error "la imagen $ref no es Java 21: $version" ;; esac
done
nota ""

nota "## 3. Lector de contadores de la JVM frente a jstat (Java 21, JVM en reposo)"
nota ""
docker run -d --name sv-cal --cpuset-cpus 1 --memory 512m --memory-swap 512m -v "$LAB/pruebas/Estable.java:/Estable.java:ro" "$IMG_JDK" \
  java -XX:+UseSerialGC -Xmx192m -Xss512k /Estable.java > /dev/null || error "no se pudo iniciar la JVM de calibración"
for i in $(seq 1 45); do docker logs sv-cal 2>&1 | grep -q '^LISTO' && break; sleep 2; done
docker logs sv-cal 2>&1 | grep -q '^LISTO' || error "la JVM de calibración no quedó lista"
sleep 5
for i in 1 2 3; do
  docker exec sv-cal jstat -gc 1 > "$SAL/jstat_$i.txt" 2>&1
  python3 "$LAB/muestreo_jvm.py" "$SAL/lector_$i.csv" --contenedor sv-cal --max-muestras 1
  nota "Muestra $i:"
  nota ""
  python3 "$LAB/comparar_jstat.py" "$SAL/jstat_$i.txt" "$SAL/lector_$i.csv" | tee -a "$REPORTE" || error "el lector de contadores no coincide con jstat en la muestra $i"
  nota ""
  sleep 3
done

nota "## 4. Interferencia del muestreo en vivo sobre el contenedor"
nota ""
cpu_cg() { docker exec sv-cal cat /sys/fs/cgroup/cpu.stat | awk '/^usage_usec/ {print $2}'; }
reloj() { date +%s.%N; }
sin_muestreo() {
  local a b t0 t1
  a="$(cpu_cg)"; t0="$(reloj)"; sleep "$VENTANA_S"; b="$(cpu_cg)"; t1="$(reloj)"
  echo "$((b - a)) $t0 $t1"
}
con_muestreo() {
  local a b t0 t1 destino="$1"
  a="$(cpu_cg)"; t0="$(reloj)"
  TIMEFORMAT='%U %S'
  { time taskset -c 3 python3 "$LAB/muestreo_jvm.py" "$SAL/interferencia_$destino.csv" --contenedor sv-cal --periodo "$PERIODO_S" --max-muestras "$MUESTRAS"; } 2> "$SAL/host_$destino.txt"
  b="$(cpu_cg)"; t1="$(reloj)"
  echo "$((b - a)) $t0 $t1"
}
S1="$(sin_muestreo)"; C1="$(con_muestreo 1)"; S2="$(sin_muestreo)"; C2="$(con_muestreo 2)"
python3 - "$S1" "$C1" "$S2" "$C2" "$MUESTRAS" "$UMBRAL_INTERFERENCIA_PCT" "$SAL" <<'PY' | tee -a "$REPORTE"
import json
import sys

def tasa(texto):
    delta, t0, t1 = texto.split()
    return float(delta) / (float(t1) - float(t0))

s1, c1, s2, c2 = (tasa(x) for x in sys.argv[1:5])
muestras, umbral, sal = int(sys.argv[5]), float(sys.argv[6]), sys.argv[7]
sin = (s1 + s2) / 2
con = (c1 + c2) / 2
extra = con - sin
ruido = abs(s1 - s2)
por_muestra = None
dur_con = [float(x.split()[2]) - float(x.split()[1]) for x in sys.argv[2:5:2]]
if muestras:
    por_muestra = extra * (sum(dur_con) / len(dur_con)) / muestras
pct = extra / 1e6 * 100
host = []
for i in (1, 2):
    try:
        u, s = open(f'{sal}/host_{i}.txt').read().split()[-2:]
        host.append(float(u) + float(s))
    except (OSError, ValueError):
        pass
veredicto = 'aceptable' if pct < umbral else 'EXCESIVA'
print('| Magnitud | Valor |')
print('|---|---|')
print(f'| CPU del contenedor sin muestreo (µs por segundo), ventanas 1 y 2 | {s1:.0f} y {s2:.0f} |')
print(f'| CPU del contenedor con muestreo (µs por segundo), ventanas 1 y 2 | {c1:.0f} y {c2:.0f} |')
print(f'| Ruido entre las dos ventanas sin muestreo (µs por segundo) | {ruido:.0f} |')
print(f'| Interferencia estimada en el contenedor (µs por segundo) | {extra:.0f} |')
print(f'| Interferencia estimada como porcentaje de 1 CPU | {pct:.3f} % |')
if por_muestra is not None:
    print(f'| Interferencia por muestra (µs) | {por_muestra:.0f} |')
if host:
    print(f'| CPU del lado del runner (muestreador y cliente de Docker) por ventana, s | {", ".join(f"{h:.2f}" for h in host)} |')
print(f'| Veredicto (criterio experimental: menos de {umbral:g} % de 1 CPU) | **{veredicto}** |')
print()
print('Una diferencia menor que el ruido entre ventanas no se distingue de cero con estas cuatro ventanas.')
json.dump({'sin_us_s': [s1, s2], 'con_us_s': [c1, c2], 'ruido_us_s': ruido, 'interferencia_us_s': extra, 'interferencia_pct_cpu': pct, 'veredicto': veredicto},
          open(f'{sal}/interferencia.json', 'w'))
sys.exit(0 if pct < umbral else 3)
PY
[ "${PIPESTATUS[0]}" = 0 ] || error "la interferencia estimada del muestreo supera el criterio experimental de $UMBRAL_INTERFERENCIA_PCT % de 1 CPU"
nota ""
docker rm -f sv-cal > /dev/null 2>&1

nota "## 5. Muestreador del generador k6 en el runner"
nota ""
mkdir -p "$SAL/gen/niveles" "$SAL/gen/servido"
python3 -m http.server 18999 --bind 127.0.0.1 --directory "$SAL/gen/servido" > /dev/null 2>&1 &
SERVIDOR=$!
sleep 2
for caso in ligero saturado; do
  etiqueta="cal_${caso}"
  nivel=10; [ "$caso" = saturado ] && nivel=20
  taskset -c 3 python3 "$LAB/muestreo_generador.py" "$SAL/gen/niveles/generador_${etiqueta}.csv" --periodo 1 &
  muestreador=$!
  taskset -c 0 k6 run --quiet -e SALIDA="$SAL/gen/niveles/k6_${etiqueta}.json" "$LAB/pruebas/k6_${caso}.js" > "$SAL/gen/k6_${caso}.log" 2>&1
  codigo=$?
  sleep 2
  kill "$muestreador" 2> /dev/null; wait "$muestreador" 2> /dev/null
  echo "{\"nivel\": $nivel, \"k6_codigo_salida\": $codigo}" > "$SAL/gen/niveles/estado_${etiqueta}.json"
done
kill "$SERVIDOR" 2> /dev/null; SERVIDOR=""
UMBRAL_GENERADOR_CPU_PCT=80 python3 "$LAB/informe_laboratorio.py" "$SAL/gen" > "$SAL/gen/SALUD_LABORATORIO.out.md" || error "informe_laboratorio.py falló"
python3 - "$SAL/gen/niveles" <<'PY' | tee -a "$REPORTE"
import json
import sys

ligero = json.load(open(f'{sys.argv[1]}/generador_cal_ligero.json', encoding='utf-8'))
saturado = json.load(open(f'{sys.argv[1]}/generador_cal_saturado.json', encoding='utf-8'))
print('| Caso | CPU de k6 (% de su núcleo): media / p95 / máx | ¿Saturado? | Usuarios activos / previstos | ¿Interpretable? |')
print('|---|---|---|---|---|')
for nombre, g in (('ligero (10 usuarios con pausa)', ligero), ('saturado a propósito (20 usuarios con cálculo)', saturado)):
    print(f"| {nombre} | {g['cpu_media_pct']:.0f} / {g['cpu_p95_pct']:.0f} / {g['cpu_max_pct']:.0f} | {g['saturado']} | {g['vus_activos_max']:.0f} / {g['nivel']} | {g['interpretable']} |")
ok = ligero['saturado'] is False and saturado['saturado'] is True and ligero['interpretable'] == 'sí' and saturado['interpretable'] == 'potencialmente no'
print()
print('Comportamiento esperado: el caso ligero no se marca y el saturado sí.' + ('' if ok else ' **No se cumplió.**'))
sys.exit(0 if ok else 4)
PY
[ "${PIPESTATUS[0]}" = 0 ] || error "el muestreador del generador no distinguió el caso ligero del saturado"
nota ""

nota "## 6. Afinidad de CPU por topología (modo opcional)"
nota ""
python3 "$LAB/afinidad.py" --cpus 1 --json "$SAL/afinidad_plan.json" > "$SAL/afinidad_plan.sh" || error "afinidad.py falló"
nota '```'
cat "$SAL/afinidad_plan.sh" | tee -a "$REPORTE"
nota '```'
python3 -c "import json,sys;[print('- ' + n) for n in json.load(open(sys.argv[1], encoding='utf-8')).get('notas', [])]" "$SAL/afinidad_plan.json" | tee -a "$REPORTE"
(
  . "$SAL/afinidad_plan.sh"
  if [ "$AFINIDAD_APLICADA" = si ]; then
    esperado_back="${PIN_BACK#--cpuset-cpus }"
    esperado_pg="${PIN_PG#--cpuset-cpus }"
    docker run -d --name sv-cal-b --cpuset-cpus "$esperado_back" --cpus 1 "$IMG_PYTHON" sleep 20 > /dev/null || exit 11
    docker run -d --name sv-cal-p --cpuset-cpus "$esperado_pg" "$IMG_PYTHON" sleep 20 > /dev/null || exit 11
    efectivo_back="$(docker exec sv-cal-b cat /sys/fs/cgroup/cpuset.cpus.effective)"
    efectivo_pg="$(docker exec sv-cal-p cat /sys/fs/cgroup/cpuset.cpus.effective)"
    taskset -c "$K6_NUCLEOS" sleep 5 &
    pid=$!
    sleep 1
    afinidad_k6="$(taskset -cp "$pid" | sed 's/.*: //')"
    wait "$pid"
    docker rm -f sv-cal-b sv-cal-p > /dev/null 2>&1
    echo "| Proceso | CPU esperadas | CPU efectivas |"
    echo "|---|---|---|"
    echo "| backend (contenedor, 1 CPU) | $esperado_back | $efectivo_back |"
    echo "| PostgreSQL (contenedor) | $esperado_pg | $efectivo_pg |"
    echo "| k6 (taskset) | $K6_NUCLEOS | $afinidad_k6 |"
    [ "$efectivo_back" = "$esperado_back" ] && [ "$efectivo_pg" = "$esperado_pg" ] && [ "$afinidad_k6" = "$K6_NUCLEOS" ] || exit 12
    python3 "$LAB/topologia.py" --pg "$esperado_pg" --backend "$esperado_back" --k6 "$K6_NUCLEOS" --mon "${PIN_MON#taskset -c }" --json "$SAL/afinidad_topologia.json" > /dev/null
    python3 -c "import json,sys;d=json.load(open(sys.argv[1]));c=[x for x in d['roles_que_comparten_nucleo_fisico'] if 'backend' in (x['a'],x['b'])];print('El backend comparte núcleo físico con otro proceso fijado: ' + ('SÍ' if c else 'no'));sys.exit(1 if c else 0)" "$SAL/afinidad_topologia.json" || exit 13
  else
    echo "El modo no se aplicó en este runner: $AFINIDAD_MOTIVO"
  fi
) | tee -a "$REPORTE"
[ "${PIPESTATUS[0]}" = 0 ] || error "la afinidad por topología no quedó como se planificó o el backend comparte núcleo físico"
nota ""

if [ "$FALLOS" = 0 ]; then nota "**Resultado: la instrumentación pasó todas las comprobaciones de este runner.**"; else nota "**Resultado: $FALLOS comprobaciones fallaron.**"; fi
nota ""
nota "Límites: es una sola ejecución en un runner de GitHub; la JVM de calibración está en reposo y no es el backend; el criterio de interferencia es experimental."
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && cat "$REPORTE" >> "$GITHUB_STEP_SUMMARY"
[ "$FALLOS" = 0 ]
