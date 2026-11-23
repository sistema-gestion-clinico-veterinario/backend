#!/bin/bash
set -uo pipefail

V="$1"
SALIDA="$SAL/visual/${ESCENARIO_VISUAL}_$V"
mkdir -p "$SALIDA"
VARIABLE_DIST="FRONT_DIST_$V"
DIST="${!VARIABLE_DIST}"

node "$RAIZ/medicion/visual/servidor_estatico.mjs" "$DIST" 4300 "$PUERTO" > "$SAL/logs/servidor_estatico_$V.log" 2>&1 &
SERVIDOR=$!
for i in $(seq 1 30); do curl -fsS "http://127.0.0.1:4300/" > /dev/null 2>&1 && break; sleep 1; done

(
  cd "$RAIZ/medicion/visual" || exit 1
  BASE="http://localhost:4300" SLUG="$SLUG" USUARIO_CUENTA="${USUARIO}1" USUARIO_BASE="$USUARIO" CLAVE="$CLAVE" \
    SALIDA="$SALIDA" ESCENARIO="$ESCENARIO_VISUAL" PUERTO_BACKEND="$PUERTO" VERSION="$V" node visual.mjs
) > "$SAL/logs/visual_${ESCENARIO_VISUAL}_$V.log" 2>&1

kill "$SERVIDOR" 2> /dev/null || true
