#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

uso() {
  cat >&2 <<'TEXTO'
Uso: restaurar-archivos.sh --archivo data-02.gpg --manifiesto meta-02.gpg --confirmo [--prefijo-destino recuperado-]

Restaura los buckets incluidos en un respaldo de Storage. Por seguridad usa, de forma predeterminada,
buckets de destino con el prefijo "recuperado-" y exige que existan y estén vacíos.
TEXTO
  exit 2
}

ARCHIVO=""; MANIFIESTO=""; CONFIRMO="no"; PREFIJO_DESTINO="recuperado-"
while [ "$#" -gt 0 ]; do
  case "$1" in
    --archivo) ARCHIVO="${2:-}"; shift 2 ;;
    --manifiesto) MANIFIESTO="${2:-}"; shift 2 ;;
    --prefijo-destino) PREFIJO_DESTINO="${2:-}"; shift 2 ;;
    --confirmo) CONFIRMO="si"; shift ;;
    *) uso ;;
  esac
done

[ -n "$ARCHIVO" ] && [ -n "$MANIFIESTO" ] || uso
[ "$CONFIRMO" = "si" ] || { echo "Falta --confirmo." >&2; exit 2; }
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO" >&2; exit 2; }
[ -f "$MANIFIESTO" ] || { echo "No existe $MANIFIESTO" >&2; exit 2; }
printf '%s' "$PREFIJO_DESTINO" | grep -Eq '^[A-Za-z0-9._-]+$' || {
  echo "El prefijo de destino contiene caracteres no permitidos." >&2
  exit 2
}

: "${SUPABASE_S3_ENDPOINT:?Falta SUPABASE_S3_ENDPOINT}"
: "${SUPABASE_S3_ACCESS_KEY:?Falta SUPABASE_S3_ACCESS_KEY}"
: "${SUPABASE_S3_SECRET_KEY:?Falta SUPABASE_S3_SECRET_KEY}"
: "${GPG_EXPECTED_SIGNING_FINGERPRINT:?Falta GPG_EXPECTED_SIGNING_FINGERPRINT}"
. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
firma_verificacion_preparar

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
MANIFIESTO_LEGIBLE="$TRABAJO/meta.json"
PAQUETE="$TRABAJO/storage.tar.gz"
EXTRAIDO="$TRABAJO/extraido"
mkdir "$EXTRAIDO"

descifrar_y_verificar_firma "$MANIFIESTO" "$MANIFIESTO_LEGIBLE" \
  "$TRABAJO/firma.status" "$TRABAJO/firma.error"
esperado="$(jq -er '.sha256' "$MANIFIESTO_LEGIBLE" | tr -d '\r')"
real="$(sha256sum "$ARCHIVO" | cut -d' ' -f1)"
[ "$esperado" = "$real" ] || { echo "El SHA-256 del respaldo de Storage no coincide." >&2; exit 3; }

gpg --batch --yes --quiet --decrypt --output "$PAQUETE" "$ARCHIVO"
if tar -tzf "$PAQUETE" | grep -Eq '(^/|(^|/)\.\.(/|$))'; then
  echo "El paquete contiene una ruta insegura y no será extraído." >&2
  exit 3
fi
tar -xzf "$PAQUETE" -C "$EXTRAIDO"

cantidad_esperada="$(jq -er '.archivos_incluidos' "$MANIFIESTO_LEGIBLE")"
cantidad_real="$(find "$EXTRAIDO" -type f | wc -l | tr -d ' ')"
[ "$cantidad_real" = "$cantidad_esperada" ] || {
  echo "Storage contiene $cantidad_real archivos; el manifiesto exige $cantidad_esperada." >&2
  exit 3
}

while IFS= read -r bucket_origen; do
  [ -n "$bucket_origen" ] || continue
  printf '%s' "$bucket_origen" | grep -Eq '^[A-Za-z0-9._-]+$' || { echo "Bucket inválido en el manifiesto." >&2; exit 3; }
  origen_local="$EXTRAIDO/$bucket_origen"
  [ -d "$origen_local" ] || { echo "Falta el bucket $bucket_origen dentro del paquete." >&2; exit 3; }
  bucket_destino="$PREFIJO_DESTINO$bucket_origen"

  export AWS_ACCESS_KEY_ID="$SUPABASE_S3_ACCESS_KEY"
  export AWS_SECRET_ACCESS_KEY="$SUPABASE_S3_SECRET_KEY"
  export AWS_DEFAULT_REGION="${SUPABASE_S3_REGION:-us-east-1}"
  aws s3api head-bucket --bucket "$bucket_destino" --endpoint-url "$SUPABASE_S3_ENDPOINT" > /dev/null 2>&1 || {
    echo "El bucket seguro de destino $bucket_destino no existe. Créalo antes de restaurar." >&2
    exit 4
  }
  if [ -n "$(aws s3 ls "s3://$bucket_destino" --recursive --endpoint-url "$SUPABASE_S3_ENDPOINT" | head -n 1)" ]; then
    echo "El bucket $bucket_destino no está vacío; se cancela para evitar sobrescrituras." >&2
    exit 4
  fi
  aws s3 sync "$origen_local" "s3://$bucket_destino" --endpoint-url "$SUPABASE_S3_ENDPOINT" --only-show-errors
done < <(jq -er '.buckets[]' "$MANIFIESTO_LEGIBLE" | tr -d '\r')

echo "Restauración de Storage completada en buckets con prefijo $PREFIJO_DESTINO"
