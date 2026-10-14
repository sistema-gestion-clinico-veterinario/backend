#!/usr/bin/env bash
set -Eeuo pipefail

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PUERTO="${PRUEBA_PUERTO:-55440}"
T="$(mktemp -d)"
PG="$T/pg"
export GNUPGHOME="$T/gnupg"
mkdir "$GNUPGHOME"
FALLAS=0

detener() {
  pg_ctl -D "$PG" -m immediate stop > /dev/null 2>&1 || true
  rm -rf "$T"
}
trap detener EXIT

verificar() {
  local descripcion="$1"; shift
  if "$@" > "$T/ultima-salida.txt" 2>&1; then
    echo "  ok   $descripcion"
  else
    echo "  FALLA $descripcion"; sed 's/^/        /' "$T/ultima-salida.txt" | tail -8
    FALLAS=$((FALLAS + 1))
  fi
}

debe_fallar() {
  local descripcion="$1"; shift
  if "$@" > "$T/ultima-salida.txt" 2>&1; then
    echo "  FALLA $descripcion (debía fallar y terminó bien)"; FALLAS=$((FALLAS + 1))
  else
    echo "  ok   $descripcion"
  fi
}

url() { echo "postgresql://postgres@localhost:$PUERTO/$1"; }

echo "Preparando un PostgreSQL temporal en el puerto $PUERTO..."
initdb -D "$PG" -U postgres --auth=trust -E UTF8 --locale=C > /dev/null
SOCKET_DIR="$T"
case "$(uname -s)" in
  MINGW*|MSYS*) SOCKET_DIR="$(cygpath -m "$T")" ;;
esac
if ! pg_ctl -D "$PG" -o "-p $PUERTO -h 127.0.0.1 -k $SOCKET_DIR" -l "$T/pg.log" -w start > /dev/null; then
  echo "PostgreSQL temporal no pudo iniciar. Log:" >&2
  sed 's/^/  /' "$T/pg.log" >&2
  exit 1
fi
for base in origen verificacion restaurada; do createdb -h localhost -p "$PUERTO" -U postgres "$base"; done

psql --dbname="$(url origen)" --no-psqlrc -q -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE company (id serial PRIMARY KEY, name text NOT NULL);
CREATE TABLE usuario (id serial PRIMARY KEY, nombre text NOT NULL, company_id int REFERENCES company(id));
CREATE TABLE historia (id serial PRIMARY KEY, usuario_id int REFERENCES usuario(id), nota text, imagen bytea);
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
CREATE INDEX idx_usuario_nombre_trgm ON usuario USING gin (lower(nombre) public.gin_trgm_ops);
INSERT INTO company(name) SELECT 'Clínica ' || g FROM generate_series(1, 3) g;
INSERT INTO usuario(nombre, company_id) SELECT 'Peña Núñez ' || g, 1 + g % 3 FROM generate_series(1, 250) g;
INSERT INTO historia(usuario_id, nota, imagen) SELECT 1 + g % 250, 'Control ñandú ' || g, decode(md5(g::text), 'hex') FROM generate_series(1, 1000) g;
SQL

echo "Generando una clave de prueba..."
gpg --batch --quiet --pinentry-mode loopback --passphrase '' --quick-generate-key "Prueba Respaldo <prueba@softvet.test>" default default never
CLAVE_PUBLICA="$(gpg --armor --export prueba@softvet.test)"
HUELLA_ESPERADA="$(gpg --batch --with-colons --list-keys prueba@softvet.test | awk -F: '/^fpr:/ {print toupper($10); exit}')"
FRASE_FIRMA="frase-temporal-de-prueba"
gpg --batch --quiet --pinentry-mode loopback --passphrase "$FRASE_FIRMA" \
  --quick-generate-key "Firma SoftVet Prueba <firma@softvet.test>" rsa2048 sign never
HUELLA_FIRMA="$(gpg --batch --with-colons --list-keys firma@softvet.test | awk -F: '/^fpr:/ {print toupper($10); exit}')"
CLAVE_FIRMA_PUBLICA="$(gpg --armor --export firma@softvet.test)"
CLAVE_FIRMA_PRIVADA="$(gpg --batch --pinentry-mode loopback --passphrase "$FRASE_FIRMA" --armor --export-secret-keys firma@softvet.test)"
SALIDA="$T/salida"

echo "Respaldo completo"
verificar "respalda, restaura en la base de verificación y cifra" env \
  BACKUP_DB_URL="$(url origen)" VERIFY_DB_URL="$(url verificacion)" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  GPG_EXPECTED_FINGERPRINT="$HUELLA_ESPERADA" \
  GPG_SIGNING_PRIVATE_KEY="$CLAVE_FIRMA_PRIVADA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  GPG_SIGNING_PASSPHRASE="$FRASE_FIRMA" \
  BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$SALIDA" bash "$AQUI/respaldar-base.sh"

CIFRADO="$SALIDA/data-01.gpg"
MANIFIESTO="$SALIDA/meta-01.gpg"
verificar "genera el archivo cifrado y su manifiesto" test -s "$CIFRADO" -a -s "$MANIFIESTO"
verificar "no deja el volcado sin cifrar" bash -c "! ls '$SALIDA' | grep -q '\.dump$'"
verificar "no deja manifiestos legibles" bash -c "! ls '$SALIDA' | grep -qE '\.manifest\.json$'"
verificar "el archivo cifrado no contiene datos legibles" bash -c "! grep -a -q 'Peña' '$CIFRADO' && ! grep -a -q 'PGDMP' '$CIFRADO'"
verificar "el manifiesto cifrado no revela su estructura" bash -c "! grep -a -q 'filas_por_tabla' '$MANIFIESTO'"
verificar "reconoce los artefactos OpenPGP bajo pipefail" bash -c \
  'set -o pipefail; source "$1/lib-destino.sh"; validar_artefacto_cifrado "$2" && validar_artefacto_cifrado "$3"' \
  _ "$AQUI" "$CIFRADO" "$MANIFIESTO"
MANIFIESTO_LEGIBLE="$T/manifest.json"
gpg --batch --yes --quiet --decrypt --output "$MANIFIESTO_LEGIBLE" "$MANIFIESTO"
echo "Manifiesto"
manifiesto_dice() { test "$(jq -r "$1" "$MANIFIESTO_LEGIBLE" | tr -d '\r')" = "$2"; }
verificar "  - restauración de verificación correcta" manifiesto_dice .restauracion_de_verificacion correcta
verificar "  - 3 tablas registradas" manifiesto_dice '.filas_por_tabla | length' 3
verificar "  - 1000 filas en historia" manifiesto_dice .filas_por_tabla.historia 1000

echo "Restauración desde el archivo cifrado"
verificar "restaura en una base nueva" env GPG_SIGNING_PUBLIC_KEY="$CLAVE_FIRMA_PUBLICA" \
  GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" bash "$AQUI/restaurar.sh" \
  --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo

valor() { psql --dbname="$(url "$1")" --no-psqlrc -At -c "$2" | tr -d '
'; }
IMAGENES='select md5(string_agg(encode(imagen, $$hex$$), $$$$ order by id)) from historia'
iguales() { test "$(valor origen "$1")" = "$(valor restaurada "$1")"; }
verificar "  - mismas filas en historia" iguales 'select count(*) from historia'
verificar "  - mismo texto con acentos" iguales "select nombre from usuario where id = 7"
verificar "  - mismos datos binarios" iguales "$IMAGENES"
verificar "  - mismas relaciones entre tablas" iguales 'select count(*) from usuario u join company c on c.id = u.company_id'
verificar "  - índice trigram restaurado después de preparar pg_trgm" iguales \
  "select coalesce(to_regclass('public.idx_usuario_nombre_trgm')::text, '')"

echo "Respaldo y restauración de Storage"
MOCK_STORAGE_RESTORE="$T/storage-restaurado"
MOCK_S3_ROOT="$T/mock-s3"
mkdir -p "$MOCK_STORAGE_RESTORE" "$MOCK_S3_ROOT/documentos/subdirectorio" "$MOCK_S3_ROOT/imagenes/subdirectorio"
printf 'documento original\n' > "$MOCK_S3_ROOT/documentos/subdirectorio/archivo.txt"
printf 'imagen original\n' > "$MOCK_S3_ROOT/imagenes/subdirectorio/archivo.txt"
export MOCK_STORAGE_RESTORE MOCK_S3_ROOT
aws() {
  if [ "${1:-} ${2:-}" = "s3 sync" ]; then
    if [[ "${3:-}" = s3://* ]]; then
      local bucket="${3#s3://}"
      mkdir -p "$4"
      cp -R "$MOCK_S3_ROOT/$bucket"/. "$4"/
      if [ "${MOCK_CORRUPT_REMOTE:-0}" = "1" ] && [[ "$bucket" = alterado-* ]]; then
        printf 'alterado durante la verificación\n' >> "$4/subdirectorio/archivo.txt"
      fi
    else
      local bucket="${4#s3://}"
      mkdir -p "$MOCK_S3_ROOT/$bucket" "$MOCK_STORAGE_RESTORE/$bucket"
      cp -R "$3"/. "$MOCK_S3_ROOT/$bucket"/
      cp -R "$3"/. "$MOCK_STORAGE_RESTORE/$bucket"/
    fi
    return 0
  fi
  if [ "${1:-} ${2:-}" = "s3api head-bucket" ]; then return 0; fi
  if [ "${1:-} ${2:-}" = "s3 ls" ]; then
    local bucket="${3#s3://}"
    [ -d "$MOCK_S3_ROOT/$bucket" ] && find "$MOCK_S3_ROOT/$bucket" -type f -print -quit
    return 0
  fi
  echo "Invocación aws no simulada: $*" >&2
  return 1
}
export -f aws

SALIDA_STORAGE="$T/salida-storage"
verificar "cifra dos buckets y genera un punto de recuperación" env \
  SUPABASE_S3_ENDPOINT="https://storage.test" SUPABASE_S3_ACCESS_KEY="prueba" SUPABASE_S3_SECRET_KEY="prueba" \
  SUPABASE_BUCKETS="documentos,imagenes" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  GPG_EXPECTED_FINGERPRINT="$HUELLA_ESPERADA" BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$SALIDA_STORAGE" \
  GPG_SIGNING_PRIVATE_KEY="$CLAVE_FIRMA_PRIVADA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  GPG_SIGNING_PASSPHRASE="$FRASE_FIRMA" \
  bash "$AQUI/respaldar-archivos.sh"
verificar "genera datos, manifiesto y marcador cifrados" test \
  -s "$SALIDA_STORAGE/data-02.gpg" -a -s "$SALIDA_STORAGE/meta-02.gpg" -a -s "$SALIDA_STORAGE/meta.gpg"
MANIFIESTO_STORAGE="$T/manifest-storage.json"
gpg --batch --yes --quiet --decrypt --output "$MANIFIESTO_STORAGE" "$SALIDA_STORAGE/meta-02.gpg"
verificar "registra los dos buckets sin exponerlos en el nombre remoto" test "$(jq '.buckets | length' "$MANIFIESTO_STORAGE")" = "2"
verificar "valida la firma del marcador de punto completo" env \
  GPG_SIGNING_PUBLIC_KEY="$CLAVE_FIRMA_PUBLICA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/verificar-punto.sh" "$SALIDA_STORAGE/meta.gpg"
verificar "restaura Storage en buckets separados de recuperación" env \
  SUPABASE_S3_ENDPOINT="https://storage.test" SUPABASE_S3_ACCESS_KEY="prueba" SUPABASE_S3_SECRET_KEY="prueba" \
  GPG_SIGNING_PUBLIC_KEY="$CLAVE_FIRMA_PUBLICA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/restaurar-archivos.sh" --archivo "$SALIDA_STORAGE/data-02.gpg" \
  --manifiesto "$SALIDA_STORAGE/meta-02.gpg" --confirmo
verificar "recupera exactamente dos archivos" test "$(find "$MOCK_STORAGE_RESTORE" -type f | wc -l | tr -d ' ')" = "2"
debe_fallar "detecta contenido remoto diferente después de restaurar Storage" env \
  MOCK_CORRUPT_REMOTE=1 SUPABASE_S3_ENDPOINT="https://storage.test" \
  SUPABASE_S3_ACCESS_KEY="prueba" SUPABASE_S3_SECRET_KEY="prueba" \
  GPG_SIGNING_PUBLIC_KEY="$CLAVE_FIRMA_PUBLICA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/restaurar-archivos.sh" --archivo "$SALIDA_STORAGE/data-02.gpg" \
  --manifiesto "$SALIDA_STORAGE/meta-02.gpg" --prefijo-destino alterado- --confirmo

echo "Casos que deben fallar"
cp "$CIFRADO" "$T/dañado.dump.gpg"
printf '\x00' | dd of="$T/dañado.dump.gpg" bs=1 seek=200 conv=notrunc 2> /dev/null
debe_fallar "un archivo dañado no se restaura (suma de verificación)" env \
  GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" bash "$AQUI/restaurar.sh" \
  --archivo "$T/dañado.dump.gpg" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo
debe_fallar "sin --confirmo no borra el destino" env GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/restaurar.sh" --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)"
mkdir "$T/sin-manifiesto"
cp "$CIFRADO" "$T/sin-manifiesto/data-01.gpg"
debe_fallar "sin manifiesto no restaura" env GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/restaurar.sh" --archivo "$T/sin-manifiesto/data-01.gpg" --destino "$(url restaurada)" --confirmo
gpg --batch --yes --quiet --trust-model always --recipient "$HUELLA_ESPERADA" \
  --output "$T/meta-sin-firma.gpg" --encrypt "$MANIFIESTO_LEGIBLE"
debe_fallar "rechaza un manifiesto cifrado pero sin firma" env \
  GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" bash "$AQUI/restaurar.sh" \
  --archivo "$CIFRADO" --manifiesto "$T/meta-sin-firma.gpg" --destino "$(url restaurada)" --confirmo
gpg --batch --yes --quiet --pinentry-mode loopback --passphrase '' --trust-model always \
  --local-user "$HUELLA_ESPERADA!" --recipient "$HUELLA_ESPERADA" \
  --output "$T/meta-firma-no-autorizada.gpg" --sign --encrypt "$MANIFIESTO_LEGIBLE"
debe_fallar "rechaza una firma válida de una clave no autorizada" env \
  GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" bash "$AQUI/restaurar.sh" \
  --archivo "$CIFRADO" --manifiesto "$T/meta-firma-no-autorizada.gpg" --destino "$(url restaurada)" --confirmo
debe_fallar "sin variables obligatorias no corre" env -u BACKUP_DB_URL bash "$AQUI/respaldar-base.sh"
printf 'contenido sin cifrar' > "$T/no-cifrado.json"
debe_fallar "el destino rechaza archivos sin cifrar" bash -c \
  'source "$1/lib-destino.sh"; destino_subir "$2" "prueba/no-cifrado.json"' _ "$AQUI" "$T/no-cifrado.json"
cp "$T/no-cifrado.json" "$T/falso.gpg"
debe_fallar "renombrar un archivo a .gpg no evade la validación" bash -c \
  'source "$1/lib-destino.sh"; destino_subir "$2" "prueba/falso.gpg"' _ "$AQUI" "$T/falso.gpg"
debe_fallar "no verifica sobre la misma base de origen" env \
  BACKUP_DB_URL="$(url origen)" VERIFY_DB_URL="$(url origen)" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  GPG_EXPECTED_FINGERPRINT="$HUELLA_ESPERADA" \
  GPG_SIGNING_PRIVATE_KEY="$CLAVE_FIRMA_PRIVADA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  GPG_SIGNING_PASSPHRASE="$FRASE_FIRMA" \
  BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$T/otra" bash "$AQUI/respaldar-base.sh"

debe_fallar "rechaza una huella de cifrado distinta" env \
  BACKUP_DB_URL="$(url origen)" VERIFY_DB_URL="$(url verificacion)" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  GPG_EXPECTED_FINGERPRINT="0000000000000000000000000000000000000000" \
  GPG_SIGNING_PRIVATE_KEY="$CLAVE_FIRMA_PRIVADA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  GPG_SIGNING_PASSPHRASE="$FRASE_FIRMA" \
  BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$T/huella-incorrecta" bash "$AQUI/respaldar-base.sh"

mkdir "$T/llavero-sin-clave-de-descifrado"
debe_fallar "sin la clave privada correcta no se puede descifrar" env GNUPGHOME="$T/llavero-sin-clave-de-descifrado" \
  GPG_SIGNING_PUBLIC_KEY="$CLAVE_FIRMA_PUBLICA" GPG_EXPECTED_SIGNING_FINGERPRINT="$HUELLA_FIRMA" \
  bash "$AQUI/restaurar.sh" --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo

echo
if [ "$FALLAS" -eq 0 ]; then echo "Todas las comprobaciones pasaron."; else echo "$FALLAS comprobaciones fallaron."; exit 1; fi
