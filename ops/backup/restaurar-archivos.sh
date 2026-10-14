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
for comando in aws gpg jq python3 sha256sum; do
  command -v "$comando" >/dev/null || { echo "Falta la herramienta obligatoria: $comando" >&2; exit 2; }
done
firma_verificacion_preparar

TRABAJO="$(mktemp -d)"
trap 'rm -rf "$TRABAJO"' EXIT
MANIFIESTO_LEGIBLE="$TRABAJO/meta.json"
PAQUETE="$TRABAJO/storage.tar.gz"
EXTRAIDO="$TRABAJO/extraido"
VERIFICACION="$TRABAJO/verificacion"
mkdir "$EXTRAIDO"
mkdir "$VERIFICACION"

descifrar_y_verificar_firma "$MANIFIESTO" "$MANIFIESTO_LEGIBLE" \
  "$TRABAJO/firma.status" "$TRABAJO/firma.error"
if ! jq -e '
  (.componente == "STORAGE") and
  (.archivo == "data-02.gpg") and
  (.sha256 | type == "string" and test("^[0-9a-fA-F]{64}$")) and
  (.archivos_incluidos | type == "number" and . >= 0 and floor == .) and
  (.buckets | type == "array" and length > 0) and
  (all(.buckets[]; type == "string" and test("^[A-Za-z0-9._-]+$"))) and
  ((.buckets | unique | length) == (.buckets | length))
' "$MANIFIESTO_LEGIBLE" >/dev/null; then
  echo "El manifiesto de Storage no tiene la estructura esperada." >&2
  exit 3
fi
esperado="$(jq -er '.sha256' "$MANIFIESTO_LEGIBLE" | tr -d '\r')"
real="$(sha256sum "$ARCHIVO" | cut -d' ' -f1)"
[ "$esperado" = "$real" ] || { echo "El SHA-256 del respaldo de Storage no coincide." >&2; exit 3; }

gpg --batch --yes --quiet --decrypt --output "$PAQUETE" "$ARCHIVO"
PAQUETE="$PAQUETE" EXTRAIDO="$EXTRAIDO" python3 - <<'PY'
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

cantidad_esperada="$(jq -er '.archivos_incluidos' "$MANIFIESTO_LEGIBLE")"
cantidad_real="$(find "$EXTRAIDO" -type f | wc -l | tr -d ' ')"
[ "$cantidad_real" = "$cantidad_esperada" ] || {
  echo "Storage contiene $cantidad_real archivos; el manifiesto exige $cantidad_esperada." >&2
  exit 3
}

mapfile -t BUCKETS < <(jq -er '.buckets[]' "$MANIFIESTO_LEGIBLE" | tr -d '\r')

mapfile -t directorios_extraidos < <(find "$EXTRAIDO" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | LC_ALL=C sort)
if [ "$(printf '%s\n' "${BUCKETS[@]}" | LC_ALL=C sort)" != "$(printf '%s\n' "${directorios_extraidos[@]}" | LC_ALL=C sort)" ]; then
  echo "Los buckets contenidos en el paquete no coinciden exactamente con el manifiesto." >&2
  exit 3
fi

export AWS_ACCESS_KEY_ID="$SUPABASE_S3_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$SUPABASE_S3_SECRET_KEY"
export AWS_DEFAULT_REGION="${SUPABASE_S3_REGION:-us-east-1}"

# Se validan todos los destinos antes de escribir el primero para evitar una restauración parcial previsible.
for bucket_origen in "${BUCKETS[@]}"; do
  [ -n "$bucket_origen" ] || continue
  bucket_destino="$PREFIJO_DESTINO$bucket_origen"
  aws s3api head-bucket --bucket "$bucket_destino" --endpoint-url "$SUPABASE_S3_ENDPOINT" > /dev/null 2>&1 || {
    echo "El bucket seguro de destino $bucket_destino no existe. Créalo antes de restaurar." >&2
    exit 4
  }
  if [ -n "$(aws s3 ls "s3://$bucket_destino" --recursive --endpoint-url "$SUPABASE_S3_ENDPOINT" | head -n 1)" ]; then
    echo "El bucket $bucket_destino no está vacío; se cancela para evitar sobrescrituras." >&2
    exit 4
  fi
done

comparar_directorios() {
  local origen="$1" remoto="$2"
  ORIGEN="$origen" REMOTO="$remoto" python3 - <<'PY'
import hashlib
import os
from pathlib import Path

def inventory(root_name):
    root = Path(os.environ[root_name])
    result = {}
    for path in sorted(root.rglob("*")):
        if path.is_symlink() or (not path.is_file() and not path.is_dir()):
            raise SystemExit(f"Entrada local no permitida: {path}")
        if path.is_file():
            digest = hashlib.sha256()
            with path.open("rb") as source:
                for block in iter(lambda: source.read(1024 * 1024), b""):
                    digest.update(block)
            result[path.relative_to(root).as_posix()] = (path.stat().st_size, digest.hexdigest())
    return result

expected = inventory("ORIGEN")
actual = inventory("REMOTO")
if expected != actual:
    missing = sorted(set(expected) - set(actual))
    extra = sorted(set(actual) - set(expected))
    changed = sorted(key for key in set(expected) & set(actual) if expected[key] != actual[key])
    print(f"Faltantes: {missing[:10]}", file=os.sys.stderr)
    print(f"Adicionales: {extra[:10]}", file=os.sys.stderr)
    print(f"Diferentes: {changed[:10]}", file=os.sys.stderr)
    raise SystemExit(1)
PY
}

for bucket_origen in "${BUCKETS[@]}"; do
  origen_local="$EXTRAIDO/$bucket_origen"
  bucket_destino="$PREFIJO_DESTINO$bucket_origen"
  verificacion_local="$VERIFICACION/$bucket_origen"
  mkdir "$verificacion_local"
  echo "Restaurando $bucket_origen en $bucket_destino..."
  aws s3 sync "$origen_local" "s3://$bucket_destino" --endpoint-url "$SUPABASE_S3_ENDPOINT" --only-show-errors
  aws s3 sync "s3://$bucket_destino" "$verificacion_local" --endpoint-url "$SUPABASE_S3_ENDPOINT" --only-show-errors
  comparar_directorios "$origen_local" "$verificacion_local" || {
    echo "La verificación remota de $bucket_destino falló." >&2
    exit 5
  }
  echo "Verificación remota correcta para $bucket_destino."
done

echo "Restauración de Storage completada y verificada en buckets con prefijo $PREFIJO_DESTINO"
