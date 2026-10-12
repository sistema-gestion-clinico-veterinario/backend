#!/usr/bin/env bash

DESTINO_TIPO="${BACKUP_DESTINO:-s3}"
DESTINO_REMOTO="${BACKUP_RCLONE_REMOTE:-respaldo}"
DESTINO_CARPETA="${BACKUP_CARPETA:-softvet-respaldos}"

destino_requisitos() {
  case "$DESTINO_TIPO" in
    s3) requerir BACKUP_S3_BUCKET BACKUP_S3_ENDPOINT AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY ;;
    drive) command -v rclone > /dev/null || { echo "Falta rclone." >&2; exit 2; } ;;
    *) echo "BACKUP_DESTINO debe ser s3 o drive." >&2; exit 2 ;;
  esac
}

destino_subir() {
  local archivo="$1" ruta="$2"
  case "$DESTINO_TIPO" in
    s3) aws s3 cp "$archivo" "s3://$BACKUP_S3_BUCKET/$ruta" --endpoint-url "$BACKUP_S3_ENDPOINT" --only-show-errors ;;
    drive) rclone copyto "$archivo" "$DESTINO_REMOTO:$DESTINO_CARPETA/$ruta" --drive-use-trash=false --quiet ;;
  esac
}

destino_tamano() {
  local ruta="$1"
  case "$DESTINO_TIPO" in
    s3) aws s3api head-object --bucket "$BACKUP_S3_BUCKET" --key "$ruta" --endpoint-url "$BACKUP_S3_ENDPOINT" \
          --query ContentLength --output text ;;
    drive) rclone lsjson "$DESTINO_REMOTO:$DESTINO_CARPETA/$ruta" --files-only 2> /dev/null | jq -r '.[0].Size // empty' | tr -d '\r' ;;
  esac
}

destino_podar() {
  local carpeta="$1" dias="$2"
  [ "$DESTINO_TIPO" = "drive" ] || return 0
  rclone delete "$DESTINO_REMOTO:$DESTINO_CARPETA/$carpeta" --min-age "${dias}d" --rmdirs --drive-use-trash=false --quiet || true
}
