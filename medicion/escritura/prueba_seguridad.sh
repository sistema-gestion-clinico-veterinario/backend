#!/bin/bash
set -uo pipefail

V="$1"
BASE="http://127.0.0.1:$PUERTO/api/v1"
TXT="$SAL/seguridad_$V.txt"
PASAN=0
FALLAN=0
N_IP=0

nueva_ip() { N_IP=$((N_IP + 1)); IP="10.251.$((N_IP / 250)).$((N_IP % 250 + 1))"; }

login() {
  curl -s -o /dev/null -w '%{http_code}' --max-time 45 -H "Content-Type: application/json" -H "Origin: http://localhost" \
    -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: $3" \
    -d "{\"slug\":\"$SLUG\",\"username\":\"$1\",\"password\":\"$2\"}" "$BASE/auth/login"
}

olvido() {
  curl -s -o "$3" -w '%{http_code}' --max-time 45 -H "Content-Type: application/json" -H "Origin: http://localhost" \
    -H "X-Company-Slug: $SLUG" -H "X-Forwarded-For: $2" \
    -d "{\"email\":\"$1\",\"slug\":\"$SLUG\"}" "$BASE/auth/forgot-password"
}

reenvio() {
  curl -s -o "$3" -w '%{http_code}' --max-time 45 -X POST -H "Origin: http://localhost" -H "X-Company-Slug: $SLUG" \
    -H "X-Forwarded-For: $2" "$BASE/auth/resend-verification?email=$1&slug=$SLUG"
}

secuencia() {
  local resultado=""
  while [ "$#" -gt 0 ]; do resultado="${resultado:+$resultado,}$1"; shift; done
  echo "$resultado"
}

comprobar() {
  local nombre="$1" esperado="$2" obtenido="$3"
  if [ "$esperado" = "$obtenido" ]; then
    PASAN=$((PASAN + 1)); echo "| $nombre | $esperado | $obtenido | CORRECTO |" >> "$TXT"
  else
    FALLAN=$((FALLAN + 1)); echo "| $nombre | $esperado | $obtenido | **FALLA** |" >> "$TXT"
  fi
}

informar() { echo "| $1 | - | $2 | informativo |" >> "$TXT"; }

consulta() { docker exec sv-pg psql -U postgres -d "$V" -tAc "$1" 2> /dev/null | tr -d '[:space:]'; }

{
  echo "## Prueba de seguridad con PostgreSQL real: $V"
  echo
  echo "Límites reales: $LIMITE_LOGIN_IP intentos por minuto por IP y $LIMITE_LOGIN_CUENTA por cuenta cada 15 minutos; bloqueo tras 5 fallos; 3 recuperaciones por cuenta por hora."
  echo
  echo "| Comprobación | Esperado | Obtenido | Resultado |"
  echo "|---|---|---|---|"
} > "$TXT"

nueva_ip
r=()
for i in 1 2 3 4 5; do r+=("$(login "${USUARIO}1" "incorrecta-$i" "$IP")"); done
comprobar "S1 cinco contraseñas incorrectas seguidas" "401,401,401,401,401" "$(secuencia "${r[@]}")"
comprobar "S1 la sexta, con la contraseña CORRECTA, sigue rechazada (cuenta bloqueada)" "401" "$(login "${USUARIO}1" "$CLAVE" "$IP")"
informar "S1 cuentas bloqueadas registradas" "$(consulta "SELECT count(*) FROM account_lockouts WHERE locked_until IS NOT NULL AND locked_until > now()")"

nueva_ip
r=()
for i in 1 2 3; do r+=("$(login "${USUARIO}2" "incorrecta-$i" "$IP")"); done
r+=("$(login "${USUARIO}2" "$CLAVE" "$IP")")
for i in 4 5 6; do r+=("$(login "${USUARIO}2" "incorrecta-$i" "$IP")"); done
r+=("$(login "${USUARIO}2" "$CLAVE" "$IP")")
comprobar "S2 tres fallos, acierto, tres fallos y acierto: el acierto limpia el contador" "401,401,401,200,401,401,401,200" "$(secuencia "${r[@]}")"

nueva_ip
r=()
for i in 1 2 3 4 5 6 7 8 9; do r+=("$(login "${USUARIO}3" "$CLAVE" "$IP")"); done
comprobar "S3 nueve inicios correctos de la misma cuenta: el noveno supera el límite" "200,200,200,200,200,200,200,200,429" "$(secuencia "${r[@]}")"

informar "S4 filas de auditoría LOGIN_FALLIDO tras S1 y S2" "$(consulta "SELECT count(*) FROM audit_logs WHERE action = 'LOGIN_FALLIDO'")"

nueva_ip
r=()
for i in 1 2 3 4; do r+=("$(olvido "${USUARIO}4@softvet.test" "$IP" "/tmp/olvido_existe_$i.json")"); done
existente="$(secuencia "${r[@]}")"
nueva_ip
r=()
for i in 1 2 3 4; do r+=("$(olvido "fantasma4@softvet.test" "$IP" "/tmp/olvido_fantasma_$i.json")"); done
fantasma="$(secuencia "${r[@]}")"
comprobar "S5 recuperación de contraseña de una cuenta existente: tres respuestas y la cuarta limitada" "200,200,200,429" "$existente"
comprobar "S5 recuperación de una cuenta inexistente: la misma secuencia (no revela si existe)" "200,200,200,429" "$fantasma"
comprobar "S5 el cuerpo de la respuesta es idéntico para cuenta existente e inexistente" "igual" \
  "$([ "$(sha1sum < /tmp/olvido_existe_1.json)" = "$(sha1sum < /tmp/olvido_fantasma_1.json)" ] && echo igual || echo distinto)"
sleep 4
informar "S5 correos recibidos en el servidor de pruebas (esperados: 3 de la cuenta existente)" "$(docker logs sv-correo 2>&1 | grep -c '^MSG')"

nueva_ip
r=()
for i in 1 2 3 4; do r+=("$(reenvio "${USUARIO}5@softvet.test" "$IP" "/tmp/reenvio_existe_$i.json")"); done
nueva_ip
f=()
for i in 1 2 3 4; do f+=("$(reenvio "fantasma5@softvet.test" "$IP" "/tmp/reenvio_fantasma_$i.json")"); done
comprobar "S6 reenvío de verificación de una cuenta ya activa" "200,200,200,429" "$(secuencia "${r[@]}")"
comprobar "S6 reenvío de una cuenta inexistente: la misma secuencia" "200,200,200,429" "$(secuencia "${f[@]}")"

rm -f /tmp/paralelo_codigos.txt
seq 1 40 | xargs -P 40 -I{} sh -c "curl -s -o /dev/null -w '%{http_code}\n' --max-time 60 -H 'Content-Type: application/json' -H 'Origin: http://localhost' -H 'X-Company-Slug: $SLUG' -H 'X-Forwarded-For: 10.252.0.{}' -d '{\"slug\":\"$SLUG\",\"username\":\"fantasma_{}\",\"password\":\"incorrecta\"}' '$BASE/auth/login' >> /tmp/paralelo_codigos.txt"
distribucion="$(sort /tmp/paralelo_codigos.txt | uniq -c | awk '{printf "%s×%s ", $2, $1}')"
comprobar "S7 cuarenta contraseñas incorrectas simultáneas de cuentas distintas (pool de $POOL): todas rechazadas con 401" "401×40" "${distribucion% }"
comprobar "S7 después de la ráfaga, el servicio responde" "200" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$BASE/actuator/health")"
nueva_ip
comprobar "S7 después de la ráfaga, un inicio de sesión correcto funciona" "200" "$(login "${USUARIO}6" "$CLAVE" "$IP")"

{
  echo
  echo "Resultado: $PASAN correctas, $FALLAN con falla."
} >> "$TXT"
cat "$TXT"
exit 0
