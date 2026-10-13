#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

uso() {
  cat >&2 <<'TEXTO'
Uso: restaurar.sh --archivo data-01.gpg --manifiesto meta-01.gpg --destino URL_DE_LA_BASE --confirmo

Descifra el respaldo con la clave privada que esté en tu llavero de GPG y lo restaura en la base indicada.
La restauración BORRA el esquema public del destino. Úsala sobre una base nueva o de pruebas.
TEXTO
  exit 2
}

ARCHIVO=""; DESTINO=""; MANIFIESTO=""; CONFIRMO="no"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --archivo) ARCHIVO="${2:-}"; shift 2 ;;
    --destino) DESTINO="${2:-}"; shift 2 ;;
    --manifiesto) MANIFIESTO="${2:-}"; shift 2 ;;
    --confirmo) CONFIRMO="si"; shift ;;
    *) uso ;;
  esac
done
[ -n "$ARCHIVO" ] && [ -n "$DESTINO" ] || uso
[ -f "$ARCHIVO" ] || { echo "No existe el archivo $ARCHIVO" >&2; exit 2; }
: "${GPG_EXPECTED_SIGNING_FINGERPRINT:?Falta GPG_EXPECTED_SIGNING_FINGERPRINT}"
. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
firma_verificacion_preparar

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
VOLCADO="$TRABAJO/restaurar.dump"
MANIFIESTO_LEGIBLE=""

if [ -z "$MANIFIESTO" ]; then
  candidato_cifrado="$(dirname "$ARCHIVO")/meta-01.gpg"
  candidato_heredado="${ARCHIVO%.dump.gpg}.manifest.json"
  if [ -f "$candidato_cifrado" ]; then
    MANIFIESTO="$candidato_cifrado"
  elif [ -f "$candidato_heredado" ]; then
    MANIFIESTO="$candidato_heredado"
  fi
fi

[ -n "$MANIFIESTO" ] || {
  echo "El manifiesto es obligatorio; no se restaurará un respaldo cuya integridad no pueda comprobarse." >&2
  exit 2
}

[ -f "$MANIFIESTO" ] || { echo "No existe el manifiesto $MANIFIESTO" >&2; exit 2; }
case "$MANIFIESTO" in
  *.gpg)
    MANIFIESTO_LEGIBLE="$TRABAJO/manifest.json"
    descifrar_y_verificar_firma "$MANIFIESTO" "$MANIFIESTO_LEGIBLE" \
      "$TRABAJO/firma.status" "$TRABAJO/firma.error"
    ;;
  *)
    [ "${ALLOW_LEGACY_UNSIGNED_MANIFEST:-0}" = "1" ] || {
      echo "El manifiesto no está firmado. Solo se admite con ALLOW_LEGACY_UNSIGNED_MANIFEST=1 para una recuperación histórica controlada." >&2
      exit 6
    }
    MANIFIESTO_LEGIBLE="$MANIFIESTO"
    echo "Aviso: recuperación excepcional de un manifiesto histórico sin firma." >&2
    ;;
esac
esperado="$(jq -er '.sha256' "$MANIFIESTO_LEGIBLE" | tr -d '\r')"
real="$(sha256sum "$ARCHIVO" | cut -d' ' -f1)"
if [ "$esperado" != "$real" ]; then
  echo "La suma de verificación no coincide: el archivo está dañado o fue modificado." >&2
  exit 3
fi
echo "Suma de verificación correcta."

destino_legible="$(printf '%s' "$DESTINO" | sed -E 's#^[a-z]+://[^@]*@##; s#\?.*$##')"
echo "Destino: $destino_legible"
if [ "$CONFIRMO" != "si" ]; then
  echo "Falta --confirmo: la restauración borra el esquema public de ese destino." >&2
  exit 2
fi

echo "Descifrando..."
gpg --batch --yes --quiet --decrypt --output "$VOLCADO" "$ARCHIVO"
pg_restore --list "$VOLCADO" > /dev/null

echo "Restaurando..."
psql --dbname="$DESTINO" --no-psqlrc -q -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
if ! pg_restore --dbname="$DESTINO" --clean --if-exists --exit-on-error --no-owner --no-acl \
    "$VOLCADO" 2> "$TRABAJO/restauracion.log"; then
  echo "La restauración falló; no se declarará el respaldo como recuperado." >&2
  head -n 20 "$TRABAJO/restauracion.log" >&2
  exit 5
fi

echo "Comparando exactamente con el manifiesto..."
fallas=0
while IFS=$'\t' read -r tabla esperadas; do
  obtenidas="$(psql --dbname="$DESTINO" --no-psqlrc -At -c "SELECT count(*) FROM public.\"$tabla\"" 2>/dev/null | tr -d '\r')"
  [ -n "$obtenidas" ] || obtenidas="ausente"
  if [ "$obtenidas" != "$esperadas" ]; then
    echo "Tabla $tabla: ${obtenidas} filas; el manifiesto exige $esperadas." >&2
    fallas=$((fallas + 1))
  fi
done < <(jq -er '.filas_por_tabla | to_entries[] | "\(.key)\t\(.value)"' "$MANIFIESTO_LEGIBLE" | tr -d '\r')
if [ "$fallas" -gt 0 ]; then
  echo "La restauración no coincide exactamente con el manifiesto en $fallas tablas." >&2
  exit 5
fi
echo "Restauración completada."
