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
resolver_plan_recuperacion
NOMBRE="softvet-base-$FECHA"
VOLCADO="$TRABAJO/$NOMBRE.dump"
ARCHIVO_CIFRADO="$SALIDA/data-01.gpg"
MANIFIESTO_CIFRADO="$SALIDA/meta-01.gpg"
CARPETA_REMOTA="$POLITICA/$RECOVERY_POINT_ID"

if [ "${BACKUP_SKIP_UPLOAD:-0}" != "1" ] && [ -z "${VERIFY_DB_URL:-}" ]; then
  echo "VERIFY_DB_URL es obligatorio antes de publicar un respaldo." >&2
  exit 2
fi

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
  host_verificacion="$(printf '%s' "$VERIFY_DB_URL" | sed -E 's#^[a-z]+://([^@]+@)?(\[[^]]+\]|[^:/?]+).*#\2#')"
  case "$host_verificacion" in
    localhost|127.0.0.1|'[::1]') ;;
    *)
      [ "${BACKUP_ALLOW_REMOTE_VERIFICATION:-0}" = "1" ] || {
        echo "Por seguridad, VERIFY_DB_URL debe apuntar a localhost. Usa BACKUP_ALLOW_REMOTE_VERIFICATION=1 solo en un entorno aislado." >&2
        exit 4
      }
      ;;
  esac
  echo "Restaurando en una base de verificación..."
  psql --dbname="$VERIFY_DB_URL" --no-psqlrc -q -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
  if ! pg_restore --dbname="$VERIFY_DB_URL" --clean --if-exists --exit-on-error --no-owner --no-acl \
      "$VOLCADO" 2> "$TRABAJO/restauracion.log"; then
    echo "La restauración de verificación falló." >&2
    head -n 20 "$TRABAJO/restauracion.log" >&2
    exit 5
  fi
  contar_filas "$VERIFY_DB_URL" > "$TRABAJO/restaurado.txt"

  tablas_origen="$(cut -d'|' -f1 "$TRABAJO/antes.txt")"
  tablas_restauradas="$(cut -d'|' -f1 "$TRABAJO/restaurado.txt")"
  if [ "$tablas_origen" != "$tablas_restauradas" ]; then
    echo "El conjunto de tablas restaurado no coincide con el origen." >&2
    exit 5
  fi
  RESULTADO_RESTAURACION="correcta"
fi

echo "Cifrando..."
cifrado_preparar
cifrar_archivo "$VOLCADO" "$ARCHIVO_CIFRADO"
rm -f "$VOLCADO"

TAMANO="$(wc -c < "$ARCHIVO_CIFRADO" | tr -d ' ')"
SHA256="$(sha256sum "$ARCHIVO_CIFRADO" | cut -d' ' -f1)"
VERSION_PG_DUMP="$(pg_dump --version | head -n1)"
CONTEO_MANIFIESTO="$TRABAJO/despues.txt"
[ -s "$TRABAJO/restaurado.txt" ] && CONTEO_MANIFIESTO="$TRABAJO/restaurado.txt"
tablas_json="$(awk -F'|' 'BEGIN{printf "{"} {printf "%s\"%s\":%s", (NR>1?",":""), $1, $2} END{printf "}"}' "$CONTEO_MANIFIESTO")"

jq -n \
  --arg recoveryPointId "$RECOVERY_POINT_ID" \
  --arg politica "$POLITICA" \
  --arg componente "BASE_DATOS" \
  --arg archivo "data-01.gpg" \
  --arg fecha "$FECHA" \
  --arg sha256 "$SHA256" \
  --argjson tamano "$TAMANO" \
  --arg herramienta "$VERSION_PG_DUMP" \
  --arg huella "$GPG_RECIPIENT_FINGERPRINT" \
  --arg huellaFirma "$GPG_SIGNING_FINGERPRINT" \
  --arg restauracion "$RESULTADO_RESTAURACION" \
  --arg commit "${GITHUB_SHA:-}" \
  --argjson filas "$tablas_json" \
  '{recovery_point_id:$recoveryPointId, politica_retencion:$politica, componente:$componente,
    archivo:$archivo, fecha_utc:$fecha, tamano_bytes:$tamano, sha256:$sha256, herramienta:$herramienta,
    clave_de_cifrado:$huella, clave_de_firma:$huellaFirma, restauracion_de_verificacion:$restauracion,
    commit:$commit, filas_por_tabla:$filas}' \
  > "$TRABAJO/$NOMBRE.manifest.json"
cifrar_y_firmar_archivo "$TRABAJO/$NOMBRE.manifest.json" "$MANIFIESTO_CIFRADO"
rm -f "$TRABAJO/$NOMBRE.manifest.json"

if [ "${BACKUP_SKIP_UPLOAD:-0}" = "1" ]; then
  echo "Subida omitida (BACKUP_SKIP_UPLOAD=1). Archivos en $SALIDA"
else
  echo "Subiendo el componente de base de datos a $CARPETA_REMOTA ..."
  destino_subir "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-01.gpg"
  destino_subir "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-01.gpg"
  destino_verificar "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-01.gpg"
  destino_verificar "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-01.gpg"
  destino_podar "p01" 30
  destino_podar "p02" 180
  destino_podar "p03" 365
fi

avisar ""
echo "Respaldo completado: $RECOVERY_POINT_ID/$POLITICA ($TAMANO bytes, restauración de verificación: $RESULTADO_RESTAURACION)"
