#!/bin/bash
set -u
RES="${1:?uso: verificar_multiclinica.sh <resultados> <clinicas>}"
K="${2:?uso: verificar_multiclinica.sh <resultados> <clinicas>}"
FALLOS=0
ok() { echo "ok   - $1"; }
mal() { echo "FALLA - $1"; FALLOS=$((FALLOS + 1)); }

ids="$(ls "$RES"/niveles/ids_despues_*.json 2> /dev/null | head -1)"
[ -n "$ids" ] || { echo "FALLA - no hay archivo de ids en $RES/niveles"; exit 1; }
nivel="$(basename "$ids" .json)"; nivel="${nivel##*_}"

n="$(jq '(.clinicas // [.]) | length' "$ids")"
[ "$n" = "$K" ] && ok "los ids traen $n clínicas" || mal "los ids traen $n clínicas y se esperaban $K"
distintas="$(jq '[(.clinicas // [.])[].companyId] | unique | length' "$ids")"
[ "$distintas" = "$K" ] && ok "las $K clínicas son empresas distintas" || mal "solo hay $distintas empresas distintas de $K"
sin_vets="$(jq '[(.clinicas // [.])[] | select((.veterinarios | length) == 0)] | length' "$ids")"
[ "$sin_vets" = 0 ] && ok "todas las clínicas tienen veterinarios" || mal "$sin_vets clínicas sin veterinarios"

csv="$RES/niveles/k6_despues_${nivel}.csv.gz"
if [ -f "$csv" ]; then
  python3 - "$csv" "$nivel" << 'PY' || FALLOS=$((FALLOS + 1))
import csv, gzip, sys
ruta, nivel = sys.argv[1], int(sys.argv[2])
estados = {}
with gzip.open(ruta, 'rt', encoding='utf-8', errors='ignore') as f:
    for fila in csv.DictReader(f):
        if fila.get('metric_name') == 'http_reqs' and '/auth/login' in (fila.get('url') or ''):
            estados[fila.get('status')] = estados.get(fila.get('status'), 0) + 1
logins_ok = estados.get('200', 0)
malos = estados.get('401', 0)
print(f'{"ok  " if malos == 0 else "FALLA"} - respuestas de login: {estados}')
print(f'{"ok  " if logins_ok >= nivel else "FALLA"} - {logins_ok} inicios de sesión correctos para {nivel} usuarios virtuales')
sys.exit(0 if (malos == 0 and logins_ok >= nivel) else 1)
PY
else
  mal "falta $csv"
fi

repetidos="$(jq -r '.documentos_repetidos // 0' "$RES/niveles/integridad_despues_${nivel}.json" 2> /dev/null || echo 0)"
[ "$repetidos" = 0 ] && ok "sin documentos de propietarios repetidos" || mal "$repetidos documentos de propietarios repetidos"

[ "$FALLOS" = 0 ] && { echo "el escenario con $K clínicas produjo datos coherentes"; exit 0; } || { echo "$FALLOS comprobaciones fallaron"; exit 1; }
