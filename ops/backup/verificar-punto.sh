#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

MARCADOR="${1:-}"
[ -f "$MARCADOR" ] || {
  echo "Uso: verificar-punto.sh RUTA/meta.gpg" >&2
  exit 2
}
: "${GPG_EXPECTED_SIGNING_FINGERPRINT:?Falta GPG_EXPECTED_SIGNING_FINGERPRINT}"

. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
firma_verificacion_preparar

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
descifrar_y_verificar_firma "$MARCADOR" "$TRABAJO/punto.json" \
  "$TRABAJO/firma.status" "$TRABAJO/firma.error"

jq -e --arg huellaFirma "$GPG_SIGNING_FINGERPRINT" '
  .estado == "COMPLETO" and
  (.clave_de_firma == $huellaFirma) and
  (.recovery_point_id | type == "string") and
  (.politica_retencion | IN("p01", "p02", "p03")) and
  (.componentes == ["data-01.gpg", "meta-01.gpg", "data-02.gpg", "meta-02.gpg"])
' "$TRABAJO/punto.json" > /dev/null || {
  echo "El marcador firmado no describe un punto de recuperación completo válido." >&2
  exit 6
}

jq '{recovery_point_id, politica_retencion, fecha_finalizacion_utc, estado, clave_de_firma, componentes}' "$TRABAJO/punto.json"
