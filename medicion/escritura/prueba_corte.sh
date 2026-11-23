#!/bin/bash
set -uo pipefail

V="$1"
BASE="http://127.0.0.1:$PUERTO/api/v1"
CICLOS="${CICLOS_CORTE:-10}"
TRABAJADORES="${TRABAJADORES_CORTE:-40}"
ESPERA="${ESPERA_CORTE_S:-12}"
TXT="$SAL/corte_$V.txt"
D="$(mktemp -d)"

recuento() {
  cat "$D"/w*.log 2> /dev/null | awk -v a="$1" -v b="$2" '
    $1 >= a && $1 < b { total++; n[$2]++ }
    END { otros = total - n["200"] - n["401"] - n["503"] - n["000"]; printf "%d %d %d %d %d %d", total, n["200"], n["401"], n["503"], n["000"], otros }'
}

trabajador() {
  local i="$1" fin="$2" codigo
  while [ ! -e "$fin" ]; do
    codigo="$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 -b "$D/j$i" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
      -H "X-Forwarded-For: 10.253.$((i / 250)).$((i % 250 + 1))" "$BASE/me/navigation")"
    echo "$(date +%s.%N) $codigo" >> "$D/w$i.log"
    sleep 0.3
  done
}

for i in $(seq 1 "$TRABAJADORES"); do
  curl -s -o /dev/null -c "$D/j$i" -H "Content-Type: application/json" -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
    -H "X-Forwarded-For: 10.253.$((i / 250)).$((i % 250 + 1))" \
    -d "{\"slug\":\"$SLUG\",\"username\":\"${USUARIO}$i\",\"password\":\"$CLAVE\"}" "$BASE/auth/login"
done

{
  echo "## Corte de PostgreSQL con peticiones en curso: $V"
  echo
  echo "$TRABAJADORES usuarios distintos consultan \`GET /me/navigation\` en bucle (una pausa de 0,3 s entre peticiones de cada uno). En cada ciclo se detiene PostgreSQL con \`docker stop\`, se mantiene detenido $ESPERA s y se vuelve a arrancar. Se cuentan las respuestas recibidas durante esos $ESPERA s."
  echo
  echo "| Ciclo | Respuestas | 200 | 401 | 503 | Sin respuesta | Otras | Conexiones cortadas con 57P01 |"
  echo "|---|---|---|---|---|---|---|---|"
} > "$TXT"

T_TOTAL=0; T_200=0; T_401=0; T_503=0; T_000=0; T_OTR=0; T_57=0
for c in $(seq 1 "$CICLOS"); do
  rm -f "$D"/w*.log "$D/fin"
  for i in $(seq 1 "$TRABAJADORES"); do trabajador "$i" "$D/fin" & done
  sleep 6
  t0="$(date +%s.%N)"
  docker stop sv-pg > /dev/null 2>&1
  sleep "$ESPERA"
  t1="$(date +%s.%N)"
  docker start sv-pg > /dev/null 2>&1
  for i in $(seq 1 120); do docker exec sv-pg pg_isready -U postgres > /dev/null 2>&1 && break; sleep 0.5; done
  for i in $(seq 1 120); do curl -fsS --max-time 2 "http://127.0.0.1:$PUERTO/api/v1/actuator/health" > /dev/null 2>&1 && break; sleep 0.5; done
  sleep 8
  touch "$D/fin"
  wait
  read -r total ok n401 n503 n000 otros <<< "$(recuento "$t0" "$t1")"
  n57="$(docker logs --since "${t0%.*}" --until "$(( ${t1%.*} + 1 ))" "sv-$V" 2>&1 | grep -c 'SQLSTATE(57P01)')"
  echo "| $c | $total | $ok | $n401 | $n503 | $n000 | $otros | $n57 |" >> "$TXT"
  T_TOTAL=$((T_TOTAL + total)); T_200=$((T_200 + ok)); T_401=$((T_401 + n401)); T_503=$((T_503 + n503)); T_000=$((T_000 + n000)); T_OTR=$((T_OTR + otros)); T_57=$((T_57 + n57))
done
echo "| **Total** | **$T_TOTAL** | **$T_200** | **$T_401** | **$T_503** | **$T_000** | **$T_OTR** | **$T_57** |" >> "$TXT"
echo >> "$TXT"
echo "Un 401 durante el corte es una sesión válida que el servidor rechazó por no poder consultar la base. Un 503 es la respuesta correcta." >> "$TXT"
rm -rf "$D"
