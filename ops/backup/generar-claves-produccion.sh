#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

uso() {
  echo "Uso: generar-claves-produccion.sh CARPETA_EXTERNA_VACIA" >&2
  exit 2
}

[ "$#" -eq 1 ] || uso
command -v gpg > /dev/null || { echo "No se encontró GPG." >&2; exit 2; }

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
RAIZ_REPOSITORIO="$(cd "$AQUI/../.." && pwd -P)"
DESTINO_SOLICITADO="$1"
if command -v cygpath > /dev/null 2>&1; then
  DESTINO_SOLICITADO="$(cygpath -u "$DESTINO_SOLICITADO")"
fi
mkdir -p "$DESTINO_SOLICITADO"
DESTINO="$(cd "$DESTINO_SOLICITADO" && pwd -P)"

case "$DESTINO/" in
  "$RAIZ_REPOSITORIO/"*)
    echo "El kit de recuperación no puede guardarse dentro del repositorio de SoftVet." >&2
    exit 2
    ;;
esac
if [ -n "$(find "$DESTINO" -mindepth 1 -maxdepth 1 -print -quit)" ]; then
  echo "La carpeta de destino debe estar vacía para evitar sobrescrituras." >&2
  exit 2
fi

read -r -s -p "Contraseña de la clave privada de DESCIFRADO: " FRASE_CIFRADO; echo
read -r -s -p "Repita la contraseña de DESCIFRADO: " FRASE_CIFRADO_2; echo
read -r -s -p "Contraseña de la clave privada de FIRMA: " FRASE_FIRMA; echo
read -r -s -p "Repita la contraseña de FIRMA: " FRASE_FIRMA_2; echo

[ "${#FRASE_CIFRADO}" -ge 16 ] || { echo "La contraseña de descifrado debe tener al menos 16 caracteres." >&2; exit 2; }
[ "${#FRASE_FIRMA}" -ge 16 ] || { echo "La contraseña de firma debe tener al menos 16 caracteres." >&2; exit 2; }
[ "$FRASE_CIFRADO" = "$FRASE_CIFRADO_2" ] || { echo "Las contraseñas de descifrado no coinciden." >&2; exit 2; }
[ "$FRASE_FIRMA" = "$FRASE_FIRMA_2" ] || { echo "Las contraseñas de firma no coinciden." >&2; exit 2; }
[ "$FRASE_CIFRADO" != "$FRASE_FIRMA" ] || { echo "Las claves de cifrado y firma deben usar contraseñas diferentes." >&2; exit 2; }

GNUPGHOME_TEMPORAL="$(mktemp -d)"
export GNUPGHOME="$GNUPGHOME_TEMPORAL"
EXITO=0
limpiar() {
  local codigo=$?
  FRASE_CIFRADO=""; FRASE_CIFRADO_2=""; FRASE_FIRMA=""; FRASE_FIRMA_2=""
  rm -rf -- "$GNUPGHOME_TEMPORAL"
  if [ "$EXITO" -ne 1 ]; then
    rm -f -- "$DESTINO/cifrado-publica.asc" "$DESTINO/cifrado-privada.asc" \
      "$DESTINO/firma-publica.asc" "$DESTINO/firma-privada.asc" \
      "$DESTINO/huellas.txt" "$DESTINO/LEEME.txt"
  fi
  exit "$codigo"
}
trap limpiar EXIT

IDENTIDAD_CIFRADO="SoftVet Backup Encryption <backup-encryption@softvet.local>"
IDENTIDAD_FIRMA="SoftVet Backup Signing <backup-signing@softvet.local>"

printf '%s\n' "$FRASE_CIFRADO" | gpg --batch --quiet --pinentry-mode loopback --passphrase-fd 0 \
  --quick-generate-key "$IDENTIDAD_CIFRADO" rsa3072 encrypt 3y
printf '%s\n' "$FRASE_FIRMA" | gpg --batch --quiet --pinentry-mode loopback --passphrase-fd 0 \
  --quick-generate-key "$IDENTIDAD_FIRMA" rsa3072 sign 3y

HUELLA_CIFRADO="$(gpg --batch --with-colons --list-keys backup-encryption@softvet.local | awk -F: '/^fpr:/ {print toupper($10); exit}')"
HUELLA_FIRMA="$(gpg --batch --with-colons --list-keys backup-signing@softvet.local | awk -F: '/^fpr:/ {print toupper($10); exit}')"
printf '%s' "$HUELLA_CIFRADO" | grep -Eq '^[A-F0-9]{40,64}$' || { echo "No se pudo obtener la huella de cifrado." >&2; exit 6; }
printf '%s' "$HUELLA_FIRMA" | grep -Eq '^[A-F0-9]{40,64}$' || { echo "No se pudo obtener la huella de firma." >&2; exit 6; }

gpg --batch --yes --armor --output "$DESTINO/cifrado-publica.asc" --export "$HUELLA_CIFRADO"
printf '%s\n' "$FRASE_CIFRADO" | gpg --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
  --armor --output "$DESTINO/cifrado-privada.asc" --export-secret-keys "$HUELLA_CIFRADO"
gpg --batch --yes --armor --output "$DESTINO/firma-publica.asc" --export "$HUELLA_FIRMA"
printf '%s\n' "$FRASE_FIRMA" | gpg --batch --yes --pinentry-mode loopback --passphrase-fd 0 \
  --armor --output "$DESTINO/firma-privada.asc" --export-secret-keys "$HUELLA_FIRMA"

printf 'BACKUP_GPG_EXPECTED_FINGERPRINT=%s\nBACKUP_GPG_SIGNING_FINGERPRINT=%s\n' \
  "$HUELLA_CIFRADO" "$HUELLA_FIRMA" > "$DESTINO/huellas.txt"
cat > "$DESTINO/LEEME.txt" <<'TEXTO'
KIT DE RECUPERACIÓN DE SOFTVET

1. Nunca subas esta carpeta al repositorio, correo o mensajería.
2. Conserva cifrado-privada.asc y firma-privada.asc en dos medios físicos cifrados.
3. GitHub Actions utiliza:
   - cifrado-publica.asc como BACKUP_GPG_PUBLIC_KEY.
   - firma-privada.asc como BACKUP_GPG_SIGNING_PRIVATE_KEY.
   - La contraseña de firma como BACKUP_GPG_SIGNING_PASSPHRASE.
   - Las huellas incluidas en huellas.txt.
4. La clave privada de descifrado y su contraseña nunca se cargan en GitHub.
5. La clave pública de firma se usa al verificar o restaurar un respaldo.
6. Estas claves caducan en tres años: planifica su rotación antes del vencimiento.
7. Realiza una prueba de restauración antes de habilitar la programación automática.
TEXTO
chmod 600 "$DESTINO"/*

for archivo in cifrado-publica.asc cifrado-privada.asc firma-publica.asc firma-privada.asc huellas.txt LEEME.txt; do
  [ -s "$DESTINO/$archivo" ] || { echo "No se generó correctamente $archivo." >&2; exit 6; }
done

EXITO=1
echo "Kit generado correctamente en: $DESTINO"
echo "Cópialo a dos medios cifrados y desconéctalos después de verificarlo."
