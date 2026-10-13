#!/usr/bin/env bash

generar_identificador_recuperacion() {
  local hexadecimal
  if [ -r /proc/sys/kernel/random/uuid ]; then
    tr '[:upper:]' '[:lower:]' < /proc/sys/kernel/random/uuid
    return
  fi
  if command -v uuidgen > /dev/null 2>&1; then
    uuidgen | tr '[:upper:]' '[:lower:]'
    return
  fi

  hexadecimal="$(od -An -N16 -tx1 /dev/urandom | tr -d '[:space:]')"
  [ "${#hexadecimal}" -eq 32 ] || {
    echo "No se pudo generar un identificador aleatorio de recuperación." >&2
    return 2
  }
  printf '%s-%s-%s-%s-%s\n' \
    "${hexadecimal:0:8}" "${hexadecimal:8:4}" "${hexadecimal:12:4}" \
    "${hexadecimal:16:4}" "${hexadecimal:20:12}"
}

resolver_plan_recuperacion() {
  local dia_semana dia_mes
  dia_semana="$(date -u +%u)"
  dia_mes="$(date -u +%d)"

  RECOVERY_POINT_ID="${BACKUP_RECOVERY_POINT_ID:-$(generar_identificador_recuperacion)}"
  printf '%s' "$RECOVERY_POINT_ID" | grep -Eq \
    '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' || {
      echo "BACKUP_RECOVERY_POINT_ID no es un identificador UUID válido." >&2
      return 2
    }

  if [ -n "${BACKUP_RETENTION_POLICY:-}" ]; then
    POLITICA="$BACKUP_RETENTION_POLICY"
  elif [ "$dia_mes" = "01" ]; then
    POLITICA="p03"
  elif [ "$dia_semana" = "7" ]; then
    POLITICA="p02"
  else
    POLITICA="p01"
  fi
  case "$POLITICA" in
    p01|p02|p03) ;;
    *) echo "Política de retención inválida: $POLITICA" >&2; return 2 ;;
  esac
}
