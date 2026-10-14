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
for comando in gpg jq pg_restore psql sha256sum; do
  command -v "$comando" >/dev/null || { echo "Falta la herramienta obligatoria: $comando" >&2; exit 2; }
done
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
if ! jq -e '
  (.componente == "BASE_DATOS") and
  (.archivo == "data-01.gpg") and
  (.sha256 | type == "string" and test("^[0-9a-fA-F]{64}$")) and
  (.filas_por_tabla | type == "object") and
  (all(.filas_por_tabla | to_entries[]; (.key | type == "string" and test("^[A-Za-z_][A-Za-z0-9_]*$")) and
    (.value | type == "number" and . >= 0 and floor == .)))
' "$MANIFIESTO_LEGIBLE" >/dev/null; then
  echo "El manifiesto de base de datos no tiene la estructura esperada." >&2
  exit 3
fi
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

restaurar_seccion() {
  local seccion="$1" descripcion="$2"
  if ! pg_restore --dbname="$DESTINO" --exit-on-error --single-transaction --no-owner --no-acl \
      --section="$seccion" "$VOLCADO" 2>> "$TRABAJO/restauracion.log"; then
    echo "Falló la restauración de $descripcion; no se declarará el respaldo como recuperado." >&2
    head -n 30 "$TRABAJO/restauracion.log" >&2
    exit 5
  fi
}

preparar_extensiones() {
  local extension
  for extension in ${RESTORE_EXTENSIONS:-pg_trgm}; do
    [[ "$extension" =~ ^[A-Za-z0-9_]+$ ]] || {
      echo "Nombre de extensión no válido: $extension" >&2
      exit 2
    }
    psql --dbname="$DESTINO" --no-psqlrc -q -v ON_ERROR_STOP=1 \
      -c "CREATE EXTENSION IF NOT EXISTS \"$extension\" WITH SCHEMA public;" \
      2>> "$TRABAJO/restauracion.log" || {
        echo "No se pudo preparar la extensión $extension." >&2
        head -n 30 "$TRABAJO/restauracion.log" >&2
        exit 5
      }
  done
}

echo "Restaurando la estructura..."
: > "$TRABAJO/restauracion.log"
psql --dbname="$DESTINO" --no-psqlrc -q -v ON_ERROR_STOP=1 \
  -c "DROP SCHEMA IF EXISTS public CASCADE;" 2>> "$TRABAJO/restauracion.log"
restaurar_seccion pre-data "la estructura"
if [ "$(psql --dbname="$DESTINO" --no-psqlrc -At -v ON_ERROR_STOP=1 -c "SELECT to_regnamespace('public') IS NOT NULL" | tr -d '\r')" != "t" ]; then
  echo "El volcado no restauró el esquema public esperado." >&2
  exit 5
fi
preparar_extensiones

echo "Restaurando los datos..."
restaurar_seccion data "los datos"

echo "Restaurando índices y restricciones..."
restaurar_seccion post-data "los índices y las restricciones"

mapfile -t tablas_manifiesto < <(jq -er '.filas_por_tabla | keys[]' "$MANIFIESTO_LEGIBLE" | tr -d '\r')
mapfile -t tablas_restauradas < <(psql --dbname="$DESTINO" --no-psqlrc -At -v ON_ERROR_STOP=1 -c \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY 1" | tr -d '\r')
if [ "$(printf '%s\n' "${tablas_manifiesto[@]}" | LC_ALL=C sort)" != "$(printf '%s\n' "${tablas_restauradas[@]}" | LC_ALL=C sort)" ]; then
  echo "El conjunto de tablas restauradas no coincide exactamente con el manifiesto." >&2
  echo "Esperadas: ${tablas_manifiesto[*]:-(ninguna)}" >&2
  echo "Restauradas: ${tablas_restauradas[*]:-(ninguna)}" >&2
  head -n 20 "$TRABAJO/restauracion.log" >&2
  exit 5
fi

echo "Comparando exactamente con el manifiesto..."
fallas=0
while IFS=$'\t' read -r tabla esperadas; do
  obtenidas="$(psql --dbname="$DESTINO" --no-psqlrc -At -v ON_ERROR_STOP=1 \
    --set=tabla="$tabla" 2>/dev/null <<'SQL' | tr -d '\r'
SELECT count(*) FROM public.:"tabla";
SQL
)"
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
