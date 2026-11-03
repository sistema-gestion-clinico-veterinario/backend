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

requerir BACKUP_RECOVERY_POINT_ID BACKUP_RETENTION_POLICY VERIFY_DB_URL \
  GPG_DECRYPTION_PRIVATE_KEY GPG_DECRYPTION_PASSPHRASE GPG_EXPECTED_FINGERPRINT \
  GPG_SIGNING_PUBLIC_KEY GPG_EXPECTED_SIGNING_FINGERPRINT

. "$(dirname "${BASH_SOURCE[0]}")/lib-destino.sh"
. "$(dirname "${BASH_SOURCE[0]}")/lib-cifrado.sh"
destino_requisitos

printf '%s' "$BACKUP_RECOVERY_POINT_ID" | grep -Eq \
  '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$' || {
  echo "BACKUP_RECOVERY_POINT_ID no es un UUID válido." >&2
  exit 2
}
printf '%s' "$BACKUP_RETENTION_POLICY" | grep -Eq '^p0[1-3]$' || {
  echo "BACKUP_RETENTION_POLICY no es válida." >&2
  exit 2
}

host_verificacion="$(printf '%s' "$VERIFY_DB_URL" | sed -E 's#^[a-z]+://([^@]+@)?(\[[^]]+\]|[^:/?]+).*#\2#')"
case "$host_verificacion" in
  localhost|127.0.0.1|'[::1]') ;;
  *)
    echo "La recuperación integral solo puede restaurar la base en localhost." >&2
    exit 4
    ;;
esac

TRABAJO="$(mktemp -d)"
export GNUPGHOME="$TRABAJO/gnupg"
mkdir -p "$GNUPGHOME" "$TRABAJO/cifrados" "$TRABAJO/restaurado/archivos"
INICIO_EPOCH="$(date +%s)"
CARPETA_REMOTA="$BACKUP_RETENTION_POLICY/$BACKUP_RECOVERY_POINT_ID"

limpiar() {
  local codigo=$?
  rm -rf "$TRABAJO"
  exit "$codigo"
}
trap limpiar EXIT

normalizar_huella() {
  printf '%s' "$1" | tr -d '[:space:]' | tr '[:lower:]' '[:upper:]'
}

HUELLA_DESCIFRADO="$(normalizar_huella "$GPG_EXPECTED_FINGERPRINT")"
HUELLA_FIRMA="$(normalizar_huella "$GPG_EXPECTED_SIGNING_FINGERPRINT")"
validar_huella_gpg "$HUELLA_DESCIFRADO" || { echo "Huella de descifrado inválida." >&2; exit 6; }
validar_huella_gpg "$HUELLA_FIRMA" || { echo "Huella de firma inválida." >&2; exit 6; }

printf '%s\n' "$GPG_DECRYPTION_PRIVATE_KEY" | gpg --batch --quiet --import
printf '%s\n' "$GPG_SIGNING_PUBLIC_KEY" | gpg --batch --quiet --import
gpg --batch --list-secret-keys --with-colons | awk -F: '/^fpr:/ {print toupper($10)}' |
  grep -Fxq "$HUELLA_DESCIFRADO" || { echo "La clave privada no coincide con la huella autorizada." >&2; exit 6; }
gpg --batch --list-keys --with-colons | awk -F: '/^fpr:/ {print toupper($10)}' |
  grep -Fxq "$HUELLA_FIRMA" || { echo "La clave de firma no coincide con la huella autorizada." >&2; exit 6; }

descifrar() {
  local origen="$1" destino="$2"
  if [ -n "${GPG_DECRYPTION_PASSPHRASE:-}" ]; then
    gpg --batch --yes --pinentry-mode loopback --passphrase-fd 3 \
      --output "$destino" --decrypt "$origen" 3<<< "$GPG_DECRYPTION_PASSPHRASE"
  else
    gpg --batch --yes --output "$destino" --decrypt "$origen"
  fi
}

descifrar_manifesto_firmado() {
  local origen="$1" destino="$2" estado="$TRABAJO/estado-gpg.txt" errores="$TRABAJO/error-gpg.txt"
  : > "$estado"
  : > "$errores"
  if [ -n "${GPG_DECRYPTION_PASSPHRASE:-}" ]; then
    gpg --batch --yes --pinentry-mode loopback --passphrase-fd 4 --status-fd 3 \
      --output "$destino" --decrypt "$origen" 3> "$estado" 2> "$errores" \
      4<<< "$GPG_DECRYPTION_PASSPHRASE" || {
        if grep -Eq 'BAD_PASSPHRASE' "$estado" || grep -Eqi 'bad passphrase|incorrect passphrase' "$errores"; then
          echo "No se pudo descifrar $(basename "$origen"): la contraseña de la clave privada no es correcta." >&2
        elif grep -Eq 'NO_SECKEY' "$estado" || grep -Eqi 'no secret key' "$errores"; then
          echo "No se pudo descifrar $(basename "$origen"): no está disponible la clave privada requerida." >&2
        elif grep -Eq 'NODATA' "$estado"; then
          echo "No se pudo leer $(basename "$origen"): el artefacto cifrado está vacío o dañado." >&2
        else
          echo "No se pudo descifrar $(basename "$origen"). Revisa la clave privada, su contraseña y la integridad del respaldo." >&2
        fi
        return 1
      }
  else
    gpg --batch --yes --status-fd 3 --output "$destino" --decrypt "$origen" \
      3> "$estado" 2> "$errores" || {
        if grep -Eq 'NO_SECKEY' "$estado" || grep -Eqi 'no secret key' "$errores"; then
          echo "No se pudo descifrar $(basename "$origen"): no está disponible la clave privada requerida." >&2
        else
          echo "No se pudo descifrar $(basename "$origen"). La clave puede requerir una contraseña o el artefacto puede estar dañado." >&2
        fi
        return 1
      }
  fi
  if grep -Eq '\[GNUPG:\] (BADSIG|ERRSIG|NO_PUBKEY)' "$estado"; then
    echo "La firma criptográfica de $(basename "$origen") no es válida o no puede verificarse." >&2
    return 1
  fi
  if ! awk '$1 == "[GNUPG:]" && $2 == "VALIDSIG" {print toupper($3)}' "$estado" |
      grep -Fxq "$HUELLA_FIRMA"; then
    echo "$(basename "$origen") fue firmado con una clave distinta de la autorizada." >&2
    return 1
  fi
}

for nombre in data-01.gpg meta-01.gpg data-02.gpg meta-02.gpg meta.gpg; do
  echo "Descargando $nombre del punto de recuperación..."
  destino_descargar "$CARPETA_REMOTA/$nombre" "$TRABAJO/cifrados/$nombre"
done

descifrar_manifesto_firmado "$TRABAJO/cifrados/meta.gpg" "$TRABAJO/punto.json" || {
  echo "No se pudo validar el marcador del punto de recuperación." >&2; exit 6;
}
jq -e --arg punto "$BACKUP_RECOVERY_POINT_ID" \
  '.recovery_point_id == $punto and .estado == "COMPLETO" and
   ((.componentes | sort) == (["data-01.gpg","data-02.gpg","meta-01.gpg","meta-02.gpg"] | sort))' \
  "$TRABAJO/punto.json" > /dev/null || {
  echo "El punto almacenado no está completo o no corresponde a la ejecución." >&2; exit 7;
}

descifrar_manifesto_firmado "$TRABAJO/cifrados/meta-01.gpg" "$TRABAJO/base.json" || {
  echo "No se pudo validar el manifiesto de base de datos." >&2; exit 6;
}
descifrar_manifesto_firmado "$TRABAJO/cifrados/meta-02.gpg" "$TRABAJO/archivos.json" || {
  echo "No se pudo validar el manifiesto de archivos." >&2; exit 6;
}

validar_componente() {
  local manifiesto="$1" componente="$2" artefacto="$3" esperado_tamano esperado_hash
  jq -e --arg punto "$BACKUP_RECOVERY_POINT_ID" --arg componente "$componente" \
    '.recovery_point_id == $punto and .componente == $componente' "$manifiesto" > /dev/null || return 1
  esperado_tamano="$(jq -r '.tamano_bytes' "$manifiesto")"
  esperado_hash="$(jq -r '.sha256' "$manifiesto")"
  [ "$(wc -c < "$artefacto" | tr -d ' ')" = "$esperado_tamano" ] || return 1
  [ "$(sha256sum "$artefacto" | cut -d' ' -f1)" = "$esperado_hash" ] || return 1
}

validar_componente "$TRABAJO/base.json" BASE_DATOS "$TRABAJO/cifrados/data-01.gpg" || {
  echo "El respaldo cifrado de base de datos no coincide con su manifiesto firmado." >&2; exit 7;
}
validar_componente "$TRABAJO/archivos.json" STORAGE "$TRABAJO/cifrados/data-02.gpg" || {
  echo "El respaldo cifrado de archivos no coincide con su manifiesto firmado." >&2; exit 7;
}

descifrar "$TRABAJO/cifrados/data-01.gpg" "$TRABAJO/restaurado/base.dump"
descifrar "$TRABAJO/cifrados/data-02.gpg" "$TRABAJO/restaurado/archivos.tar.gz"
pg_restore --list "$TRABAJO/restaurado/base.dump" > /dev/null

echo "Restaurando la base descargada en PostgreSQL aislado..."
psql --dbname="$VERIFY_DB_URL" --no-psqlrc -q -v ON_ERROR_STOP=1 \
  -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"
pg_restore --dbname="$VERIFY_DB_URL" --clean --if-exists --exit-on-error --no-owner --no-acl \
  --section=pre-data "$TRABAJO/restaurado/base.dump"
for extension in ${BACKUP_VERIFY_EXTENSIONS:-pg_trgm}; do
  printf '%s' "$extension" | grep -Eq '^[A-Za-z0-9_]+$' || exit 5
  psql --dbname="$VERIFY_DB_URL" --no-psqlrc -q -v ON_ERROR_STOP=1 \
    -c "CREATE EXTENSION IF NOT EXISTS \"$extension\" WITH SCHEMA public;"
done
pg_restore --dbname="$VERIFY_DB_URL" --exit-on-error --no-owner --no-acl \
  --section=data "$TRABAJO/restaurado/base.dump"
pg_restore --dbname="$VERIFY_DB_URL" --exit-on-error --no-owner --no-acl \
  --section=post-data "$TRABAJO/restaurado/base.dump"

mapfile -t TABLAS_MANIFIESTO < <(jq -er '.filas_por_tabla | keys[]' "$TRABAJO/base.json" | tr -d '\r')
mapfile -t TABLAS_RESTAURADAS < <(psql --dbname="$VERIFY_DB_URL" --no-psqlrc -At -v ON_ERROR_STOP=1 -c \
  "SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY 1" | tr -d '\r')
if [ "$(printf '%s\n' "${TABLAS_MANIFIESTO[@]}" | LC_ALL=C sort)" != \
     "$(printf '%s\n' "${TABLAS_RESTAURADAS[@]}" | LC_ALL=C sort)" ]; then
  echo "El conjunto de tablas restauradas no coincide exactamente con el manifiesto." >&2
  echo "Esperadas: ${TABLAS_MANIFIESTO[*]:-(ninguna)}" >&2
  echo "Restauradas: ${TABLAS_RESTAURADAS[*]:-(ninguna)}" >&2
  exit 5
fi

jq -r '.filas_por_tabla | to_entries[] | [.key, (.value|tostring)] | @tsv' "$TRABAJO/base.json" |
while IFS=$'\t' read -r tabla esperado; do
  printf '%s' "$tabla" | grep -Eq '^[A-Za-z0-9_]+$' || exit 5
  actual="$(psql --dbname="$VERIFY_DB_URL" --no-psqlrc -At \
    -c "SELECT count(*) FROM public.\"$tabla\"" | tr -d '\r')"
  [ "$actual" = "$esperado" ] || {
    echo "La tabla $tabla debía contener $esperado filas y restauró $actual." >&2
    exit 5
  }
done

echo "Extrayendo los archivos en un directorio aislado..."
PAQUETE="$TRABAJO/restaurado/archivos.tar.gz" \
EXTRAIDO="$TRABAJO/restaurado/archivos" python3 - <<'PY'
import os
import shutil
import tarfile
from pathlib import Path, PurePosixPath

archive_path = Path(os.environ["PAQUETE"])
destination = Path(os.environ["EXTRAIDO"])
seen = set()

with tarfile.open(archive_path, "r:gz") as archive:
    for member in archive.getmembers():
        path = PurePosixPath(member.name)
        parts = tuple(part for part in path.parts if part not in ("", "."))
        if not parts and member.isdir():
            continue
        if path.is_absolute() or ".." in parts or not parts:
            raise SystemExit(f"Ruta insegura en el paquete: {member.name!r}")
        if not (member.isdir() or member.isfile()):
            raise SystemExit(f"Tipo de entrada no permitido en el paquete: {member.name!r}")
        relative = Path(*parts)
        if relative in seen:
            raise SystemExit(f"Entrada duplicada en el paquete: {member.name!r}")
        seen.add(relative)
        target = destination / relative
        if member.isdir():
            target.mkdir(parents=True, exist_ok=True)
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        source = archive.extractfile(member)
        if source is None:
            raise SystemExit(f"No se pudo leer la entrada: {member.name!r}")
        with source, target.open("xb") as output:
            shutil.copyfileobj(source, output)
PY
ESPERADOS="$(jq -r '.archivos_incluidos' "$TRABAJO/archivos.json")"
RESTAURADOS="$(find "$TRABAJO/restaurado/archivos" -type f | wc -l | tr -d ' ')"
[ "$RESTAURADOS" = "$ESPERADOS" ] || {
  echo "Se esperaban $ESPERADOS archivos y se restauraron $RESTAURADOS." >&2; exit 5;
}
mapfile -t BUCKETS_MANIFIESTO < <(jq -er '.buckets[]' "$TRABAJO/archivos.json" | tr -d '\r')
mapfile -t BUCKETS_RESTAURADOS < <(find "$TRABAJO/restaurado/archivos" -mindepth 1 -maxdepth 1 \
  -type d -printf '%f\n' | LC_ALL=C sort)
if [ "$(printf '%s\n' "${BUCKETS_MANIFIESTO[@]}" | LC_ALL=C sort)" != \
     "$(printf '%s\n' "${BUCKETS_RESTAURADOS[@]}" | LC_ALL=C sort)" ]; then
  echo "Los buckets extraídos no coinciden exactamente con el manifiesto." >&2
  exit 5
fi

DURACION="$(( $(date +%s) - INICIO_EPOCH ))"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "integral_recovery_verified=true" >> "$GITHUB_OUTPUT"
  echo "integral_recovery_duration_seconds=$DURACION" >> "$GITHUB_OUTPUT"
fi
echo "Recuperación integral verificada desde el repositorio remoto: base y $RESTAURADOS archivos en ${DURACION}s."
