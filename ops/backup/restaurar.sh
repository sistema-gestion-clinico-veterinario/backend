#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

uso() {
  cat >&2 <<'TEXTO'
Uso: restaurar.sh --archivo RESPALDO.dump.gpg --destino URL_DE_LA_BASE --confirmo [--manifiesto RESPALDO.manifest.json]

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

if [ -z "$MANIFIESTO" ]; then
  candidato="${ARCHIVO%.dump.gpg}.manifest.json"
  [ -f "$candidato" ] && MANIFIESTO="$candidato"
fi

if [ -n "$MANIFIESTO" ]; then
  esperado="$(jq -r '.sha256' "$MANIFIESTO" | tr -d '\r')"
  real="$(sha256sum "$ARCHIVO" | cut -d' ' -f1)"
  if [ "$esperado" != "$real" ]; then
    echo "La suma de verificación no coincide: el archivo está dañado o fue modificado." >&2
    exit 3
  fi
  echo "Suma de verificación correcta."
else
  echo "Aviso: sin manifiesto no se puede comprobar la integridad del archivo antes de descifrarlo." >&2
fi

destino_legible="$(printf '%s' "$DESTINO" | sed -E 's#^[a-z]+://[^@]*@##; s#\?.*$##')"
echo "Destino: $destino_legible"
if [ "$CONFIRMO" != "si" ]; then
  echo "Falta --confirmo: la restauración borra el esquema public de ese destino." >&2
  exit 2
fi

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
VOLCADO="$TRABAJO/restaurar.dump"

echo "Descifrando..."
gpg --batch --yes --quiet --decrypt --output "$VOLCADO" "$ARCHIVO"
pg_restore --list "$VOLCADO" > /dev/null

echo "Restaurando..."
psql --dbname="$DESTINO" --no-psqlrc -q -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
pg_restore --dbname="$DESTINO" --clean --if-exists --no-owner --no-acl "$VOLCADO" 2> "$TRABAJO/restauracion.log" || true

if [ -n "$MANIFIESTO" ]; then
  echo "Comparando con el manifiesto..."
  fallas=0
  while IFS=$'\t' read -r tabla esperadas; do
    obtenidas="$(psql --dbname="$DESTINO" --no-psqlrc -At -c "SELECT count(*) FROM public.\"$tabla\"" 2>/dev/null | tr -d '\r')"
    [ -n "$obtenidas" ] || obtenidas="ausente"
    if [ "$obtenidas" = "ausente" ]; then
      echo "Falta la tabla $tabla" >&2; fallas=$((fallas + 1))
    elif [ "$obtenidas" -lt "$((esperadas * 99 / 100))" ] || [ "$obtenidas" -gt "$((esperadas * 101 / 100 + 5))" ]; then
      echo "Tabla $tabla: $obtenidas filas, el manifiesto registra $esperadas" >&2; fallas=$((fallas + 1))
    fi
  done < <(jq -r '.filas_por_tabla | to_entries[] | "\(.key)\t\(.value)"' "$MANIFIESTO" | tr -d '\r')
  if [ "$fallas" -gt 0 ]; then
    echo "La restauración no coincide con el manifiesto en $fallas tablas. Revisa $TRABAJO/restauracion.log" >&2
    cp "$TRABAJO/restauracion.log" "./restauracion-error.log" 2>/dev/null || true
    exit 5
  fi
fi
echo "Restauración completada."
