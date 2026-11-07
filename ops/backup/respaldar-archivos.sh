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

requerir SUPABASE_S3_ENDPOINT SUPABASE_S3_ACCESS_KEY SUPABASE_S3_SECRET_KEY GPG_PUBLIC_KEY GPG_EXPECTED_FINGERPRINT \
  GPG_SIGNING_PRIVATE_KEY GPG_EXPECTED_SIGNING_FINGERPRINT GPG_SIGNING_PASSPHRASE
. "$(dirname "${BASH_SOURCE[0]}")/lib-destino.sh"
. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
. "$(dirname "${BASH_SOURCE[0]}")/lib-plan-respaldo.sh"
BUCKET_CONFIG="${SUPABASE_BUCKETS:-${SUPABASE_BUCKET:-}}"
[ -n "$BUCKET_CONFIG" ] || { echo "Falta SUPABASE_BUCKETS (o SUPABASE_BUCKET por compatibilidad)." >&2; exit 2; }
if [ "${BACKUP_SKIP_UPLOAD:-0}" != "1" ]; then
  destino_requisitos
fi

TRABAJO="$(mktemp -d)"
SALIDA="${BACKUP_OUT_DIR:-$TRABAJO/salida}"
mkdir -p "$SALIDA"
export GNUPGHOME="$TRABAJO/gnupg"
mkdir "$GNUPGHOME"

descargar_bucket() {
  local bucket="$1"
  local destino="$2"
  local max_intentos="${BACKUP_STORAGE_MAX_ATTEMPTS:-4}"
  local espera_base="${BACKUP_STORAGE_RETRY_DELAY_SECONDS:-15}"
  local intento=1
  local espera
  local log_error="$TRABAJO/storage-download-error.log"

  printf '%s' "$max_intentos" | grep -Eq '^[1-9][0-9]*$' || {
    echo "BACKUP_STORAGE_MAX_ATTEMPTS debe ser un entero mayor que cero." >&2
    return 2
  }
  printf '%s' "$espera_base" | grep -Eq '^[0-9]+$' || {
    echo "BACKUP_STORAGE_RETRY_DELAY_SECONDS debe ser un entero no negativo." >&2
    return 2
  }

  while [ "$intento" -le "$max_intentos" ]; do
    rm -rf "$destino"
    mkdir -p "$destino"
    : > "$log_error"

    if AWS_ACCESS_KEY_ID="$SUPABASE_S3_ACCESS_KEY" \
      AWS_SECRET_ACCESS_KEY="$SUPABASE_S3_SECRET_KEY" \
      AWS_DEFAULT_REGION="${SUPABASE_S3_REGION:-us-east-1}" \
      AWS_RETRY_MODE="${AWS_RETRY_MODE:-standard}" \
      AWS_MAX_ATTEMPTS="${AWS_MAX_ATTEMPTS:-10}" \
      AWS_EC2_METADATA_DISABLED=true \
      aws s3 sync "s3://$bucket" "$destino" \
        --endpoint-url "$SUPABASE_S3_ENDPOINT" \
        --cli-connect-timeout "${BACKUP_STORAGE_CONNECT_TIMEOUT_SECONDS:-30}" \
        --cli-read-timeout "${BACKUP_STORAGE_READ_TIMEOUT_SECONDS:-120}" \
        --only-show-errors 2> "$log_error"; then
      rm -f "$log_error"
      return 0
    fi

    echo "La descarga de archivos falló en el intento $intento de $max_intentos." >&2
    if [ "$intento" -lt "$max_intentos" ]; then
      espera=$((espera_base * intento))
      echo "Se reintentará la descarga completa en ${espera} segundos." >&2
      sleep "$espera"
    fi
    intento=$((intento + 1))
  done

  echo "Supabase Storage no permitió completar la descarga después de $max_intentos intentos." >&2
  tail -n 25 "$log_error" >&2 || true
  return 1
}

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
resolver_plan_recuperacion
NOMBRE="softvet-archivos-$FECHA"
ARCHIVO_CIFRADO="$SALIDA/data-02.gpg"
MANIFIESTO_CIFRADO="$SALIDA/meta-02.gpg"
MARCADOR_COMPLETO="$SALIDA/meta.gpg"
CARPETA_REMOTA="$POLITICA/$RECOVERY_POINT_ID"

# Validar claves y huellas antes de descargar Storage o crear el paquete.
cifrado_preparar
avisar "/start"

mkdir "$TRABAJO/archivos"
IFS=',' read -r -a BUCKETS_SOLICITADOS <<< "$BUCKET_CONFIG"
BUCKETS=()
for bucket_solicitado in "${BUCKETS_SOLICITADOS[@]}"; do
  bucket="$(printf '%s' "$bucket_solicitado" | xargs)"
  [ -n "$bucket" ] || continue
  printf '%s' "$bucket" | grep -Eq '^[A-Za-z0-9._-]+$' || {
    echo "Nombre de bucket no permitido: $bucket" >&2
    exit 2
  }
  BUCKETS+=("$bucket")
  echo "Descargando los archivos del bucket configurado..."
  descargar_bucket "$bucket" "$TRABAJO/archivos/$bucket"
done
[ "${#BUCKETS[@]}" -gt 0 ] || { echo "No se configuró ningún bucket válido." >&2; exit 2; }

CANTIDAD="$(find "$TRABAJO/archivos" -type f | wc -l | tr -d ' ')"
echo "Archivos descargados: $CANTIDAD"

tar -C "$TRABAJO/archivos" -czf "$TRABAJO/$NOMBRE.tar.gz" .
cifrar_archivo "$TRABAJO/$NOMBRE.tar.gz" "$ARCHIVO_CIFRADO"
rm -f "$TRABAJO/$NOMBRE.tar.gz"

TAMANO="$(wc -c < "$ARCHIVO_CIFRADO" | tr -d ' ')"
SHA256="$(sha256sum "$ARCHIVO_CIFRADO" | cut -d' ' -f1)"
BUCKETS_JSON="$(printf '%s\n' "${BUCKETS[@]}" | jq -R . | jq -s .)"
jq -n --arg recoveryPointId "$RECOVERY_POINT_ID" --arg politica "$POLITICA" --arg componente "STORAGE" \
  --arg archivo "data-02.gpg" --arg fecha "$FECHA" --arg sha256 "$SHA256" --argjson tamano "$TAMANO" \
  --argjson cantidad "$CANTIDAD" --arg huella "$GPG_RECIPIENT_FINGERPRINT" \
  --arg huellaFirma "$GPG_SIGNING_FINGERPRINT" --argjson buckets "$BUCKETS_JSON" \
  '{recovery_point_id:$recoveryPointId, politica_retencion:$politica, componente:$componente,
    archivo:$archivo, fecha_utc:$fecha, tamano_bytes:$tamano, sha256:$sha256, archivos_incluidos:$cantidad,
    buckets:$buckets, clave_de_cifrado:$huella, clave_de_firma:$huellaFirma}' \
  > "$TRABAJO/$NOMBRE.manifest.json"
cifrar_y_firmar_archivo "$TRABAJO/$NOMBRE.manifest.json" "$MANIFIESTO_CIFRADO"
rm -f "$TRABAJO/$NOMBRE.manifest.json"

jq -n --arg recoveryPointId "$RECOVERY_POINT_ID" --arg politica "$POLITICA" --arg fecha "$FECHA" \
  --arg huellaFirma "$GPG_SIGNING_FINGERPRINT" \
  '{recovery_point_id:$recoveryPointId, politica_retencion:$politica, fecha_finalizacion_utc:$fecha,
    estado:"COMPLETO", clave_de_firma:$huellaFirma,
    componentes:["data-01.gpg","meta-01.gpg","data-02.gpg","meta-02.gpg","data-03.gpg","meta-03.gpg"]}' \
  > "$TRABAJO/punto-recuperacion.json"
cifrar_y_firmar_archivo "$TRABAJO/punto-recuperacion.json" "$MARCADOR_COMPLETO"
rm -f "$TRABAJO/punto-recuperacion.json"

if [ "${BACKUP_SKIP_UPLOAD:-0}" = "1" ]; then
  echo "Subida omitida (BACKUP_SKIP_UPLOAD=1). Archivos en $SALIDA"
else
  if [ "${BACKUP_REQUIRE_DATABASE_COMPONENT:-1}" = "1" ]; then
    TAMANO_BASE="$(destino_tamano "$CARPETA_REMOTA/data-01.gpg" 2> /dev/null || true)"
    TAMANO_META_BASE="$(destino_tamano "$CARPETA_REMOTA/meta-01.gpg" 2> /dev/null || true)"
    TAMANO_LOGS="$(destino_tamano "$CARPETA_REMOTA/data-03.gpg" 2> /dev/null || true)"
    TAMANO_META_LOGS="$(destino_tamano "$CARPETA_REMOTA/meta-03.gpg" 2> /dev/null || true)"
    printf '%s' "$TAMANO_BASE" | grep -Eq '^[1-9][0-9]*$' &&
      printf '%s' "$TAMANO_META_BASE" | grep -Eq '^[1-9][0-9]*$' &&
      printf '%s' "$TAMANO_LOGS" | grep -Eq '^[1-9][0-9]*$' &&
      printf '%s' "$TAMANO_META_LOGS" | grep -Eq '^[1-9][0-9]*$' || {
        echo "No se publicará un punto completo sin sus componentes de base de datos y logs." >&2
        exit 7
      }
  fi
  destino_subir "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-02.gpg"
  destino_subir "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-02.gpg"
  destino_verificar "$ARCHIVO_CIFRADO" "$CARPETA_REMOTA/data-02.gpg"
  destino_verificar "$MANIFIESTO_CIFRADO" "$CARPETA_REMOTA/meta-02.gpg"
  # Este marcador se publica al final: su presencia indica que los tres componentes terminaron.
  destino_subir "$MARCADOR_COMPLETO" "$CARPETA_REMOTA/meta.gpg"
  destino_verificar "$MARCADOR_COMPLETO" "$CARPETA_REMOTA/meta.gpg"
  destino_podar "p01" 30
  destino_podar "p02" 180
  destino_podar "p03" 365
fi

avisar ""
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "protected_size_bytes=$TAMANO" >> "$GITHUB_OUTPUT"
  echo "protected_file_count=$CANTIDAD" >> "$GITHUB_OUTPUT"
fi
echo "Punto de recuperación completado: $RECOVERY_POINT_ID/$POLITICA ($CANTIDAD archivos, $TAMANO bytes)"
