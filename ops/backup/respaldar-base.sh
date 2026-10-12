#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

requerir() {
  local faltantes=()
  for nombre in "$@"; do
    [ -n "${!nombre:-}" ] || faltantes+=("$nombre")
  done
  if [ "${#faltantes[@]}" -gt 0 ]; then
    echo "Faltan variables obligatorias: ${faltantes[*]}" >&2
    exit 2
  fi
}

requerir BACKUP_DB_URL GPG_PUBLIC_KEY
. "$(dirname "${BASH_SOURCE[0]}")/lib-destino.sh"
if [ "${BACKUP_SKIP_UPLOAD:-0}" != "1" ]; then
  destino_requisitos
fi

TRABAJO="$(mktemp -d)"
SALIDA="${BACKUP_OUT_DIR:-$TRABAJO/salida}"
mkdir -p "$SALIDA"
export GNUPGHOME="$TRABAJO/gnupg"
mkdir "$GNUPGHOME"

avisar() {
  [ -n "${HEALTHCHECK_URL:-}" ] || return 0
  curl -fsS -m 15 --retry 3 -o /dev/null "${HEALTHCHECK_URL}${1:-}" || true
}

limpiar() {
  local codigo=$?
  rm -rf "$TRABAJO"
  if [ "$codigo" -ne 0 ]; then
    echo "El respaldo falló (código $codigo)." >&2
    avisar "/fail"
  fi
  exit "$codigo"
}
trap limpiar EXIT

FECHA="$(date -u +%Y-%m-%dT%H%M%SZ)"
DIA="$(date -u +%Y-%m-%d)"
DIA_SEMANA="$(date -u +%u)"
DIA_MES="$(date -u +%d)"
NOMBRE="softvet-base-$FECHA"
VOLCADO="$TRABAJO/$NOMBRE.dump"

avisar "/start"

contar_filas() {
  local url="$1" tablas tabla
  tablas="$(psql --dbname="$url" --no-psqlrc -At -c \
    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY 1" | tr -d '\r')"
  while IFS= read -r tabla; do
    [ -n "$tabla" ] || continue
    printf '%s|%s\n' "$tabla" "$(psql --dbname="$url" --no-psqlrc -At -c "SELECT count(*) FROM public.\"$tabla\"" | tr -d '\r')"
  done <<< "$tablas"
}

echo "Contando filas antes del volcado..."
contar_filas "$BACKUP_DB_URL" > "$TRABAJO/antes.txt"
[ -s "$TRABAJO/antes.txt" ] || { echo "La base no tiene tablas en el esquema public." >&2; exit 3; }

echo "Generando el volcado..."
pg_dump --dbname="$BACKUP_DB_URL" --format=custom --compress=9 --no-owner --no-acl --schema=public --file="$VOLCADO"
pg_restore --list "$VOLCADO" > /dev/null

contar_filas "$BACKUP_DB_URL" > "$TRABAJO/despues.txt"

RESULTADO_RESTAURACION="no ejecutada"
if [ -n "${VERIFY_DB_URL:-}" ]; then
  destino="$(printf '%s' "$VERIFY_DB_URL" | sed -E 's#^[a-z]+://[^@]*@##; s#\?.*$##')"
  origen="$(printf '%s' "$BACKUP_DB_URL" | sed -E 's#^[a-z]+://[^@]*@##; s#\?.*$##')"
  if [ "$destino" = "$origen" ]; then
    echo "VERIFY_DB_URL apunta a la misma base que BACKUP_DB_URL; no se restaura sobre el origen." >&2
    exit 4
  fi
  echo "Restaurando en una base de verificación..."
  psql --dbname="$VERIFY_DB_URL" --no-psqlrc -q -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
  pg_restore --dbname="$VERIFY_DB_URL" --clean --if-exists --no-owner --no-acl "$VOLCADO" 2> "$TRABAJO/restauracion.log" || true
  contar_filas "$VERIFY_DB_URL" > "$TRABAJO/restaurado.txt"

  discrepancias=0
  while IFS='|' read -r tabla antes; do
    despues="$(awk -F'|' -v t="$tabla" '$1 == t {print $2}' "$TRABAJO/despues.txt")"
    restaurado="$(awk -F'|' -v t="$tabla" '$1 == t {print $2}' "$TRABAJO/restaurado.txt")"
    if [ -z "$restaurado" ]; then
      echo "Falta la tabla $tabla en la restauración." >&2
      discrepancias=$((discrepancias + 1))
      continue
    fi
    minimo=$antes; maximo=${despues:-$antes}
    if [ "$minimo" -gt "$maximo" ]; then minimo=$maximo; maximo=$antes; fi
    if [ "$restaurado" -lt "$minimo" ] || [ "$restaurado" -gt "$maximo" ]; then
      echo "Tabla $tabla: restauradas $restaurado filas, esperadas entre $minimo y $maximo." >&2
      discrepancias=$((discrepancias + 1))
    fi
  done < "$TRABAJO/antes.txt"
  if [ "$discrepancias" -gt 0 ]; then
    echo "La restauración de verificación no coincide con el origen ($discrepancias tablas)." >&2
    head -n 20 "$TRABAJO/restauracion.log" >&2
    exit 5
  fi
  RESULTADO_RESTAURACION="correcta"
fi

echo "Cifrando..."
printf '%s\n' "$GPG_PUBLIC_KEY" | gpg --batch --quiet --import
HUELLA="$(gpg --batch --list-keys --with-colons | awk -F: '/^fpr:/ {print $10; exit}')"
[ -n "$HUELLA" ] || { echo "No se pudo leer la clave pública de cifrado." >&2; exit 6; }
gpg --batch --yes --quiet --trust-model always --cipher-algo AES256 --compress-algo none \
  --recipient "$HUELLA" --output "$SALIDA/$NOMBRE.dump.gpg" --encrypt "$VOLCADO"
rm -f "$VOLCADO"

TAMANO="$(wc -c < "$SALIDA/$NOMBRE.dump.gpg" | tr -d ' ')"
SHA256="$(sha256sum "$SALIDA/$NOMBRE.dump.gpg" | cut -d' ' -f1)"
VERSION_PG_DUMP="$(pg_dump --version | head -n1)"
tablas_json="$(awk -F'|' 'BEGIN{printf "{"} {printf "%s\"%s\":%s", (NR>1?",":""), $1, $2} END{printf "}"}' "$TRABAJO/despues.txt")"

jq -n \
  --arg archivo "$NOMBRE.dump.gpg" \
  --arg fecha "$FECHA" \
  --arg sha256 "$SHA256" \
  --argjson tamano "$TAMANO" \
  --arg herramienta "$VERSION_PG_DUMP" \
  --arg huella "$HUELLA" \
  --arg restauracion "$RESULTADO_RESTAURACION" \
  --arg commit "${GITHUB_SHA:-}" \
  --argjson filas "$tablas_json" \
  '{archivo:$archivo, fecha_utc:$fecha, tamano_bytes:$tamano, sha256:$sha256, herramienta:$herramienta,
    clave_de_cifrado:$huella, restauracion_de_verificacion:$restauracion, commit:$commit, filas_por_tabla:$filas}' \
  > "$SALIDA/$NOMBRE.manifest.json"

if [ "${BACKUP_SKIP_UPLOAD:-0}" = "1" ]; then
  echo "Subida omitida (BACKUP_SKIP_UPLOAD=1). Archivos en $SALIDA"
else
  destinos=("base/diario/$DIA")
  [ "$DIA_SEMANA" = "7" ] && destinos+=("base/semanal/$DIA")
  [ "$DIA_MES" = "01" ] && destinos+=("base/mensual/$DIA")
  for carpeta in "${destinos[@]}"; do
    echo "Subiendo a $carpeta ..."
    destino_subir "$SALIDA/$NOMBRE.dump.gpg" "$carpeta/$NOMBRE.dump.gpg"
    destino_subir "$SALIDA/$NOMBRE.manifest.json" "$carpeta/$NOMBRE.manifest.json"
    remoto="$(destino_tamano "$carpeta/$NOMBRE.dump.gpg")"
    if [ "$remoto" != "$TAMANO" ]; then
      echo "El tamaño subido (${remoto:-ninguno}) no coincide con el local ($TAMANO)." >&2
      exit 7
    fi
  done
  destino_podar "base/diario" 8
  destino_podar "base/semanal" 31
  destino_podar "base/mensual" 190
fi

avisar ""
echo "Respaldo completado: $NOMBRE ($TAMANO bytes, restauración de verificación: $RESULTADO_RESTAURACION)"
