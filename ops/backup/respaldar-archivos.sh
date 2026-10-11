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

requerir SUPABASE_S3_ENDPOINT SUPABASE_S3_ACCESS_KEY SUPABASE_S3_SECRET_KEY SUPABASE_BUCKET GPG_PUBLIC_KEY
requerir BACKUP_S3_BUCKET BACKUP_S3_ENDPOINT AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY

TRABAJO="$(mktemp -d)"
export GNUPGHOME="$TRABAJO/gnupg"
mkdir "$GNUPGHOME"

avisar() {
  [ -n "${HEALTHCHECK_ARCHIVOS_URL:-}" ] || return 0
  curl -fsS -m 15 --retry 3 -o /dev/null "${HEALTHCHECK_ARCHIVOS_URL}${1:-}" || true
}

limpiar() {
  local codigo=$?
  rm -rf "$TRABAJO"
  if [ "$codigo" -ne 0 ]; then
    echo "El respaldo de archivos falló (código $codigo)." >&2
    avisar "/fail"
  fi
  exit "$codigo"
}
trap limpiar EXIT

FECHA="$(date -u +%Y-%m-%dT%H%M%SZ)"
DIA="$(date -u +%Y-%m-%d)"
NOMBRE="softvet-archivos-$FECHA"
avisar "/start"

echo "Descargando los archivos del bucket $SUPABASE_BUCKET ..."
mkdir "$TRABAJO/archivos"
AWS_ACCESS_KEY_ID="$SUPABASE_S3_ACCESS_KEY" AWS_SECRET_ACCESS_KEY="$SUPABASE_S3_SECRET_KEY" AWS_DEFAULT_REGION="${SUPABASE_S3_REGION:-us-east-1}" \
  aws s3 sync "s3://$SUPABASE_BUCKET" "$TRABAJO/archivos" --endpoint-url "$SUPABASE_S3_ENDPOINT" --only-show-errors

CANTIDAD="$(find "$TRABAJO/archivos" -type f | wc -l | tr -d ' ')"
echo "Archivos descargados: $CANTIDAD"

tar -C "$TRABAJO/archivos" -czf "$TRABAJO/$NOMBRE.tar.gz" .
printf '%s\n' "$GPG_PUBLIC_KEY" | gpg --batch --quiet --import
HUELLA="$(gpg --batch --list-keys --with-colons | awk -F: '/^fpr:/ {print $10; exit}')"
[ -n "$HUELLA" ] || { echo "No se pudo leer la clave pública de cifrado." >&2; exit 6; }
gpg --batch --yes --quiet --trust-model always --cipher-algo AES256 --compress-algo none \
  --recipient "$HUELLA" --output "$TRABAJO/$NOMBRE.tar.gz.gpg" --encrypt "$TRABAJO/$NOMBRE.tar.gz"
rm -f "$TRABAJO/$NOMBRE.tar.gz"

TAMANO="$(wc -c < "$TRABAJO/$NOMBRE.tar.gz.gpg" | tr -d ' ')"
SHA256="$(sha256sum "$TRABAJO/$NOMBRE.tar.gz.gpg" | cut -d' ' -f1)"
jq -n --arg archivo "$NOMBRE.tar.gz.gpg" --arg fecha "$FECHA" --arg sha256 "$SHA256" --argjson tamano "$TAMANO" \
  --argjson cantidad "$CANTIDAD" --arg huella "$HUELLA" \
  '{archivo:$archivo, fecha_utc:$fecha, tamano_bytes:$tamano, sha256:$sha256, archivos_incluidos:$cantidad, clave_de_cifrado:$huella}' \
  > "$TRABAJO/$NOMBRE.manifest.json"

for carpeta in "archivos/semanal/$DIA"; do
  aws s3 cp "$TRABAJO/$NOMBRE.tar.gz.gpg" "s3://$BACKUP_S3_BUCKET/$carpeta/$NOMBRE.tar.gz.gpg" --endpoint-url "$BACKUP_S3_ENDPOINT" --only-show-errors
  aws s3 cp "$TRABAJO/$NOMBRE.manifest.json" "s3://$BACKUP_S3_BUCKET/$carpeta/$NOMBRE.manifest.json" --endpoint-url "$BACKUP_S3_ENDPOINT" --only-show-errors
  remoto="$(aws s3api head-object --bucket "$BACKUP_S3_BUCKET" --key "$carpeta/$NOMBRE.tar.gz.gpg" \
    --endpoint-url "$BACKUP_S3_ENDPOINT" --query ContentLength --output text)"
  if [ "$remoto" != "$TAMANO" ]; then
    echo "El tamaño subido ($remoto) no coincide con el local ($TAMANO)." >&2
    exit 7
  fi
done

avisar ""
echo "Respaldo de archivos completado: $NOMBRE ($CANTIDAD archivos, $TAMANO bytes)"
