#!/usr/bin/env bash
set -Eeuo pipefail

readonly API_SUPABASE="https://api.supabase.com/v1"
readonly CIDRS_V4_BASE="${SUPABASE_DB_ALLOWED_CIDRS_BASE:-[]}"
readonly CIDRS_V6_BASE="${SUPABASE_DB_ALLOWED_CIDRS_V6_BASE:-[]}"

uso() {
  cat >&2 <<'TEXTO'
Uso: acceso-temporal-supabase.sh autorizar|restaurar

Autoriza temporalmente la IPv4 pública del runner o restaura la lista base.
Las listas base deben ser arreglos JSON de CIDR, por ejemplo:
  SUPABASE_DB_ALLOWED_CIDRS_BASE='["203.0.113.10/32"]'
  SUPABASE_DB_ALLOWED_CIDRS_V6_BASE='[]'
TEXTO
  exit 2
}

requerir() {
  local nombre
  for nombre in "$@"; do
    [ -n "${!nombre:-}" ] || {
      echo "Falta la variable obligatoria: $nombre" >&2
      exit 2
    }
  done
}

validar_cidrs() {
  local familia="$1" json="$2"
  jq -e 'type == "array" and all(.[]; type == "string")' <<<"$json" >/dev/null || {
    echo "La lista base de IPv${familia} debe ser un arreglo JSON de textos." >&2
    exit 2
  }

  CIDRS_JSON="$json" FAMILIA="$familia" python3 - <<'PY'
import ipaddress
import json
import os
import sys

family = int(os.environ["FAMILIA"])
for value in json.loads(os.environ["CIDRS_JSON"]):
    try:
        network = ipaddress.ip_network(value, strict=False)
    except ValueError as error:
        print(f"CIDR inválido: {value}: {error}", file=sys.stderr)
        raise SystemExit(2)
    if network.version != family:
        print(f"{value} no corresponde a IPv{family}.", file=sys.stderr)
        raise SystemExit(2)
    if network.prefixlen == 0:
        print(f"No se permite una regla abierta /0 en la lista base: {value}", file=sys.stderr)
        raise SystemExit(2)
PY
}

obtener_ipv4_runner() {
  local ip
  ip="$(curl --fail --silent --show-error --proto '=https' --max-time 15 \
    https://checkip.amazonaws.com | tr -d '[:space:]')"
  RUNNER_IPV4="$ip" python3 - <<'PY'
import ipaddress
import os

address = ipaddress.ip_address(os.environ["RUNNER_IPV4"])
if address.version != 4 or not address.is_global:
    raise SystemExit(f"La dirección obtenida no es una IPv4 pública válida: {address}")
PY
  printf '%s' "$ip"
}

aplicar_restricciones() {
  local cidrs_v4="$1" cidrs_v6="$2" respuesta codigo cuerpo
  local intento salida_curl espera
  local -r max_intentos=5
  respuesta="$(mktemp)"

  for ((intento = 1; intento <= max_intentos; intento++)); do
    : >"$respuesta"
    salida_curl=0
    codigo="$(curl --silent --show-error --connect-timeout 10 --max-time 25 \
      --output "$respuesta" --write-out '%{http_code}' \
      --request POST \
      --header "Authorization: Bearer $SUPABASE_MANAGEMENT_ACCESS_TOKEN" \
      --header 'Content-Type: application/json' \
      --data "$(jq -cn --argjson v4 "$cidrs_v4" --argjson v6 "$cidrs_v6" \
        '{dbAllowedCidrs: $v4, dbAllowedCidrsV6: $v6}')" \
      "$API_SUPABASE/projects/$SUPABASE_PROJECT_REF/network-restrictions/apply")" || salida_curl=$?

    if [ "$salida_curl" -eq 0 ] && [ "$codigo" = "201" ]; then
      rm -f -- "$respuesta"
      return 0
    fi

    # La operación reemplaza la configuración completa y es idempotente. Es seguro
    # repetirla cuando el gateway falla o limita temporalmente las solicitudes.
    if [ "$salida_curl" -ne 0 ] || [ "$codigo" = "429" ] || [[ "$codigo" =~ ^5[0-9][0-9]$ ]]; then
      if [ "$intento" -lt "$max_intentos" ]; then
        espera=$((2 ** intento))
        if [ "$salida_curl" -ne 0 ]; then
          echo "No se pudo contactar la API de Supabase. Reintento $((intento + 1))/$max_intentos en ${espera}s..." >&2
        else
          echo "Supabase respondió HTTP $codigo. Reintento $((intento + 1))/$max_intentos en ${espera}s..." >&2
        fi
        sleep "$espera"
        continue
      fi
    fi

    cuerpo="$(jq -c '{message, error, statusCode}' "$respuesta" 2>/dev/null || printf '%s' 'respuesta no JSON')"
    rm -f -- "$respuesta"
    if [ "$salida_curl" -ne 0 ]; then
      echo "No se pudo contactar la API de Supabase después de $intento intentos (curl $salida_curl)." >&2
    else
      echo "Supabase rechazó la actualización de restricciones (HTTP $codigo): $cuerpo" >&2
    fi
    return 3
  done
}

esperar_restricciones() {
  local esperadas_v4="$1" esperadas_v6="$2" respuesta actuales_v4 actuales_v6 intento
  respuesta="$(mktemp)"
  for intento in {1..12}; do
    if curl --fail --silent --show-error \
      --header "Authorization: Bearer $SUPABASE_MANAGEMENT_ACCESS_TOKEN" \
      "$API_SUPABASE/projects/$SUPABASE_PROJECT_REF/network-restrictions" >"$respuesta"; then
      actuales_v4="$(jq -c '.config.dbAllowedCidrs // [] | sort' "$respuesta")"
      actuales_v6="$(jq -c '.config.dbAllowedCidrsV6 // [] | sort' "$respuesta")"
      if [ "$actuales_v4" = "$(jq -c 'sort' <<<"$esperadas_v4")" ] && \
         [ "$actuales_v6" = "$(jq -c 'sort' <<<"$esperadas_v6")" ]; then
        rm -f -- "$respuesta"
        return 0
      fi
    fi
    sleep 5
  done
  rm -f -- "$respuesta"
  echo "Supabase no confirmó las restricciones esperadas dentro de 60 segundos." >&2
  return 4
}

requerir SUPABASE_MANAGEMENT_ACCESS_TOKEN SUPABASE_PROJECT_REF
command -v curl >/dev/null || { echo "Falta curl." >&2; exit 2; }
command -v jq >/dev/null || { echo "Falta jq." >&2; exit 2; }
command -v python3 >/dev/null || { echo "Falta python3." >&2; exit 2; }

validar_cidrs 4 "$CIDRS_V4_BASE"
validar_cidrs 6 "$CIDRS_V6_BASE"
[ "$(jq 'length' <<<"$CIDRS_V4_BASE")" -gt 0 ] || \
[ "$(jq 'length' <<<"$CIDRS_V6_BASE")" -gt 0 ] || {
  echo "Debe existir al menos un CIDR base; una lista vacía dejaría PostgreSQL sin restricción de red." >&2
  exit 2
}

case "${1:-}" in
  autorizar)
    ipv4_runner="$(obtener_ipv4_runner)"
    [ "${GITHUB_ACTIONS:-false}" != "true" ] || echo "::add-mask::$ipv4_runner"
    cidr_runner="${ipv4_runner}/32"
    cidrs_v4="$(jq -cn --argjson base "$CIDRS_V4_BASE" --arg temporal "$cidr_runner" \
      '$base + [$temporal] | unique')"
    echo "Autorizando temporalmente la IPv4 del runner..."
    aplicar_restricciones "$cidrs_v4" "$CIDRS_V6_BASE"
    esperar_restricciones "$cidrs_v4" "$CIDRS_V6_BASE"
    echo "Acceso temporal confirmado."
    ;;
  restaurar)
    echo "Restaurando las restricciones permanentes de Supabase..."
    aplicar_restricciones "$CIDRS_V4_BASE" "$CIDRS_V6_BASE"
    esperar_restricciones "$CIDRS_V4_BASE" "$CIDRS_V6_BASE"
    echo "Restricciones permanentes restauradas."
    ;;
  *) uso ;;
esac
