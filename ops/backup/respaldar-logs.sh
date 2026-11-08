#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

requerir() {
  local faltantes=()
  for nombre in "$@"; do
    [ -n "${!nombre:-}" ] || faltantes+=("$nombre")
  done
  [ "${#faltantes[@]}" -eq 0 ] || {
    echo "Faltan variables obligatorias: ${faltantes[*]}" >&2
    exit 2
  }
}

requerir BACKUP_DB_URL GPG_PUBLIC_KEY GPG_EXPECTED_FINGERPRINT \
  GPG_SIGNING_PRIVATE_KEY GPG_EXPECTED_SIGNING_FINGERPRINT GPG_SIGNING_PASSPHRASE
. "$(dirname "${BASH_SOURCE[0]}")/lib-destino.sh"
. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
. "$(dirname "${BASH_SOURCE[0]}")/lib-plan-respaldo.sh"
if [ "${BACKUP_SKIP_UPLOAD:-0}" != "1" ]; then
  destino_requisitos
fi

TRABAJO="$(mktemp -d)"
SALIDA="${BACKUP_OUT_DIR:-$TRABAJO/salida}"
mkdir -p "$SALIDA" "$TRABAJO/logs"
export GNUPGHOME="$TRABAJO/gnupg"
mkdir "$GNUPGHOME"
trap 'codigo=$?; rm -rf "$TRABAJO"; exit "$codigo"' EXIT

FECHA="$(date -u +%Y-%m-%dT%H%M%SZ)"
PERIODO_HASTA="${BACKUP_LOGS_TO:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
PERIODO_DESDE="${BACKUP_LOGS_FROM:-1970-01-01T00:00:00Z}"
resolver_plan_recuperacion
NOMBRE="softvet-logs-$FECHA"
ARCHIVO_CIFRADO="$SALIDA/data-03.gpg"
MANIFIESTO_CIFRADO="$SALIDA/meta-03.gpg"
CARPETA_REMOTA="$POLITICA/$RECOVERY_POINT_ID"

cifrado_preparar

AUDIT_FILE="$TRABAJO/logs/audit_logs.jsonl"
SYSTEM_FILE="$TRABAJO/logs/system_logs.jsonl"

echo "Exportando auditoría funcional sin credenciales ni cuerpos de solicitudes..."
psql --dbname="$BACKUP_DB_URL" --no-psqlrc -v ON_ERROR_STOP=1 -qAt \
  -v periodo_desde="$PERIODO_DESDE" -v periodo_hasta="$PERIODO_HASTA" \
  --output="$AUDIT_FILE" -c "SELECT row_to_json(registro) FROM (
    SELECT id, timestamp, correlation_id, outcome, user_email, user_role,
           company_id, company_name, action, module, details, ip_address, dispositivo,
           business_trace_id, entity_type, entity_id, previous_value, new_value
    FROM audit_logs
    WHERE timestamp >= :'periodo_desde'::timestamptz
      AND timestamp < :'periodo_hasta'::timestamptz
    ORDER BY id
  ) registro"

echo "Exportando trazabilidad técnica sanitizada..."
psql --dbname="$BACKUP_DB_URL" --no-psqlrc -v ON_ERROR_STOP=1 -qAt \
  -v periodo_desde="$PERIODO_DESDE" -v periodo_hasta="$PERIODO_HASTA" \
  --output="$SYSTEM_FILE" -c "SELECT row_to_json(registro) FROM (
    SELECT id, timestamp, correlation_id, category, event, outcome, http_method,
           request_path, status_code, duration_ms, user_email, company_id,
           ip_address, dispositivo, error_type
    FROM system_logs
    WHERE timestamp >= :'periodo_desde'::timestamptz
      AND timestamp < :'periodo_hasta'::timestamptz
    ORDER BY id
  ) registro"

AUDIT_COUNT="$(wc -l < "$AUDIT_FILE" | tr -d ' ')"
SYSTEM_COUNT="$(wc -l < "$SYSTEM_FILE" | tr -d ' ')"
AUDIT_SHA="$(sha256sum "$AUDIT_FILE" | cut -d' ' -f1)"
SYSTEM_SHA="$(sha256sum "$SYSTEM_FILE" | cut -d' ' -f1)"

tar -C "$TRABAJO/logs" -czf "$TRABAJO/$NOMBRE.tar.gz" audit_logs.jsonl system_logs.jsonl
cifrar_archivo "$TRABAJO/$NOMBRE.tar.gz" "$ARCHIVO_CIFRADO"
rm -f "$TRABAJO/$NOMBRE.tar.gz"

TAMANO="$(wc -c < "$ARCHIVO_CIFRADO" | tr -d ' ')"
SHA256="$(sha256sum "$ARCHIVO_CIFRADO" | cut -d' ' -f1)"
jq -n \
  --arg recoveryPointId "$RECOVERY_POINT_ID" --arg politica "$POLITICA" \
  --arg componente "LOGS" --arg archivo "data-03.gpg" --arg fecha "$FECHA" \
  --arg periodoDesde "$PERIODO_DESDE" --arg periodoHasta "$PERIODO_HASTA" \
  --arg sha256 "$SHA256" --argjson tamano "$TAMANO" \
  --argjson auditCount "$AUDIT_COUNT" --argjson systemCount "$SYSTEM_COUNT" \
  --arg auditSha "$AUDIT_SHA" --arg systemSha "$SYSTEM_SHA" \
  --arg huella "$GPG_RECIPIENT_FINGERPRINT" --arg huellaFirma "$GPG_SIGNING_FINGERPRINT" \
  '{recovery_point_id:$recoveryPointId,politica_retencion:$politica,componente:$componente,
    archivo:$archivo,fecha_utc:$fecha,tamano_bytes:$tamano,sha256:$sha256,
    periodo:{desde_inclusivo:$periodoDesde,hasta_exclusivo:$periodoHasta},
    formato:"JSON Lines UTF-8 dentro de tar.gz",registros:{audit_logs:$auditCount,system_logs:$systemCount},
    archivos:{"audit_logs.jsonl":$auditSha,"system_logs.jsonl":$systemSha},
    clave_de_cifrado:$huella,clave_de_firma:$huellaFirma}' \
  > "$TRABAJO/$NOMBRE.manifest.json"
cifrar_y_firmar_archivo "$TRABAJO/$NOMBRE.manifest.json" "$MANIFIESTO_CIFRADO"
rm -f "$TRABAJO/$NOMBRE.manifest.json"

if [ "${BACKUP_SKIP_UPLOAD:-0}" = "1" ]; then
  echo "Subida omitida (BACKUP_SKIP_UPLOAD=1)."
else
  destino_subir "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-03.gpg"
  destino_subir "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-03.gpg"
  destino_verificar "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-03.gpg"
  destino_verificar "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-03.gpg"
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "logs_protected_size_bytes=$TAMANO" >> "$GITHUB_OUTPUT"
  echo "protected_log_count=$((AUDIT_COUNT + SYSTEM_COUNT))" >> "$GITHUB_OUTPUT"
fi
echo "Logs respaldados: $AUDIT_COUNT eventos funcionales y $SYSTEM_COUNT técnicos ($TAMANO bytes)."
