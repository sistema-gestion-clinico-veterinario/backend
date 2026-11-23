#!/bin/bash
set -u
D="${1:?uso: verificar_humo.sh <carpeta de resultados>}"
N="$D/niveles"
FALLOS=0
ok() { echo "ok   - $1"; }
mal() { echo "FALLA - $1"; FALLOS=$((FALLOS + 1)); }
comprobar() { if eval "$2"; then ok "$1"; else mal "$1"; fi; }

comprobar "el perfil experimental queda en el estado del nivel" "grep -q '\"perfil_lab\": \"EXP-HEAP-256M' \"$N\"/estado_despues_10.json"
comprobar "el heap máximo es 256m" "grep -q '\"heap_max\": \"256m\"' \"$N\"/estado_despues_10.json"
comprobar "el contenedor tiene 512 MB" "grep -q '\"memoria_limite_mb\": 512' \"$N\"/estado_despues_10.json"
comprobar "el pool queda registrado" "grep -q '^pool de conexiones: 20' \"$D\"/entorno.txt"
comprobar "el muestreo en vivo trae RssAnon e hilos" "python3 - \"$N/jvm_vivo_despues_10.csv\" <<'PY'
import csv, sys
filas = list(csv.DictReader(open(sys.argv[1], encoding='utf-8')))
sys.exit(0 if any(f.get('rss_anon_kb') and f.get('hilos_so') for f in filas) else 1)
PY"
comprobar "la actividad de PostgreSQL tiene al menos 5 muestras" "[ \$(wc -l < \"$N/pg_actividad_despues_10.csv\") -ge 6 ]"
comprobar "hay un volcado de hilos con el encabezado de la JVM" "gzip -dc \"$N\"/hilos_despues_10_t*.txt.gz | head -50 | grep -q 'Full thread dump'"
comprobar "el volcado contiene hilos de peticiones" "gzip -dc \"$N\"/hilos_despues_10_t*.txt.gz | grep -q 'http-nio-8080-exec'"
comprobar "hay resumen de memoria nativa (NMT)" "grep -q 'Native Memory Tracking' \"$N/nmt_despues_10.txt\""
comprobar "el registro del backend trae estadísticas de Hikari" "grep -q 'Pool stats' \"$D/logs/backend_carga_despues_10.log\""
comprobar "k6 entregó los usuarios previstos" "grep -q '\"vus_activos_max\": 10' \"$N/k6_despues_10.json\""
comprobar "el informe del laboratorio se generó" "[ -s \"$D/SALUD_LABORATORIO.md\" ]"
echo "--- análisis del volcado:"
python3 "$(dirname "$0")/analizar_hilos.py" "$N" || true
[ "$FALLOS" = 0 ] && echo "todas las comprobaciones pasaron" || { echo "$FALLOS comprobaciones fallaron"; exit 1; }
