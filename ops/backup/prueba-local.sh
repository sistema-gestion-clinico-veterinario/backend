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
pg_ctl -D "$PG" -o "-p $PUERTO" -l "$T/pg.log" -w start > /dev/null
for base in origen verificacion restaurada; do createdb -h localhost -p "$PUERTO" -U postgres "$base"; done

psql --dbname="$(url origen)" --no-psqlrc -q -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE company (id serial PRIMARY KEY, name text NOT NULL);
CREATE TABLE usuario (id serial PRIMARY KEY, nombre text NOT NULL, company_id int REFERENCES company(id));
CREATE TABLE historia (id serial PRIMARY KEY, usuario_id int REFERENCES usuario(id), nota text, imagen bytea);
INSERT INTO company(name) SELECT 'Clínica ' || g FROM generate_series(1, 3) g;
INSERT INTO usuario(nombre, company_id) SELECT 'Peña Núñez ' || g, 1 + g % 3 FROM generate_series(1, 250) g;
INSERT INTO historia(usuario_id, nota, imagen) SELECT 1 + g % 250, 'Control ñandú ' || g, decode(md5(g::text), 'hex') FROM generate_series(1, 1000) g;
SQL

echo "Generando una clave de prueba..."
gpg --batch --quiet --pinentry-mode loopback --passphrase '' --quick-generate-key "Prueba Respaldo <prueba@softvet.test>" default default never
CLAVE_PUBLICA="$(gpg --armor --export prueba@softvet.test)"
SALIDA="$T/salida"

echo "Respaldo completo"
verificar "respalda, restaura en la base de verificación y cifra" env \
  BACKUP_DB_URL="$(url origen)" VERIFY_DB_URL="$(url verificacion)" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$SALIDA" bash "$AQUI/respaldar-base.sh"

CIFRADO="$(ls "$SALIDA"/*.dump.gpg 2>/dev/null | head -1 || true)"
MANIFIESTO="${CIFRADO%.dump.gpg}.manifest.json"
verificar "genera el archivo cifrado y su manifiesto" test -s "$CIFRADO" -a -s "$MANIFIESTO"
verificar "no deja el volcado sin cifrar" bash -c "! ls '$SALIDA' | grep -q '\.dump$'"
verificar "el archivo cifrado no contiene datos legibles" bash -c "! grep -a -q 'Peña' '$CIFRADO' && ! grep -a -q 'PGDMP' '$CIFRADO'"
manifiesto_dice() { test "$(jq -r "$1" "$MANIFIESTO" | tr -d '\r')" = "$2"; }
echo "Manifiesto"
verificar "  - restauración de verificación correcta" manifiesto_dice .restauracion_de_verificacion correcta
verificar "  - 3 tablas registradas" manifiesto_dice '.filas_por_tabla | length' 3
verificar "  - 1000 filas en historia" manifiesto_dice .filas_por_tabla.historia 1000

echo "Restauración desde el archivo cifrado"
verificar "restaura en una base nueva" bash "$AQUI/restaurar.sh" --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo

valor() { psql --dbname="$(url "$1")" --no-psqlrc -At -c "$2" | tr -d '
'; }
IMAGENES='select md5(string_agg(encode(imagen, $$hex$$), $$$$ order by id)) from historia'
iguales() { test "$(valor origen "$1")" = "$(valor restaurada "$1")"; }
verificar "  - mismas filas en historia" iguales 'select count(*) from historia'
verificar "  - mismo texto con acentos" iguales "select nombre from usuario where id = 7"
verificar "  - mismos datos binarios" iguales "$IMAGENES"
verificar "  - mismas relaciones entre tablas" iguales 'select count(*) from usuario u join company c on c.id = u.company_id'

echo "Casos que deben fallar"
cp "$CIFRADO" "$T/dañado.dump.gpg"
printf '\x00' | dd of="$T/dañado.dump.gpg" bs=1 seek=200 conv=notrunc 2> /dev/null
debe_fallar "un archivo dañado no se restaura (suma de verificación)" bash "$AQUI/restaurar.sh" \
  --archivo "$T/dañado.dump.gpg" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo
debe_fallar "sin --confirmo no borra el destino" bash "$AQUI/restaurar.sh" --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)"
debe_fallar "sin variables obligatorias no corre" env -u BACKUP_DB_URL bash "$AQUI/respaldar-base.sh"
debe_fallar "no verifica sobre la misma base de origen" env \
  BACKUP_DB_URL="$(url origen)" VERIFY_DB_URL="$(url origen)" GPG_PUBLIC_KEY="$CLAVE_PUBLICA" \
  BACKUP_SKIP_UPLOAD=1 BACKUP_OUT_DIR="$T/otra" bash "$AQUI/respaldar-base.sh"

GNUPGHOME="$T/otro-llavero" bash -c 'mkdir "$GNUPGHOME"; gpg --batch --quiet --pinentry-mode loopback --passphrase "" --quick-generate-key "Otra <otra@x.test>" default default never' > /dev/null 2>&1
debe_fallar "sin la clave privada correcta no se puede descifrar" env GNUPGHOME="$T/otro-llavero" \
  bash "$AQUI/restaurar.sh" --archivo "$CIFRADO" --manifiesto "$MANIFIESTO" --destino "$(url restaurada)" --confirmo

echo
if [ "$FALLAS" -eq 0 ]; then echo "Todas las comprobaciones pasaron."; else echo "$FALLAS comprobaciones fallaron."; exit 1; fi
