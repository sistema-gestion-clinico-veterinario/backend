#!/usr/bin/env bash


normalizar_huella_gpg() {
  printf '%s' "$1" | tr -d '[:space:]' | tr '[:lower:]' '[:upper:]'
}

validar_huella_gpg() {
  printf '%s' "$1" | grep -Eq '^[A-F0-9]{40,64}$'
}

cifrado_preparar() {
  local huella_esperada huellas_importadas huella_firma huellas_privadas
  huella_esperada="$(printf '%s' "$GPG_EXPECTED_FINGERPRINT" | tr -d '[:space:]' | tr '[:lower:]' '[:upper:]')"
  validar_huella_gpg "$huella_esperada" || {
    echo "GPG_EXPECTED_FINGERPRINT no contiene una huella GPG válida." >&2
    return 6
  }

  huella_firma="$(normalizar_huella_gpg "$GPG_EXPECTED_SIGNING_FINGERPRINT")"
  validar_huella_gpg "$huella_firma" || {
    echo "GPG_EXPECTED_SIGNING_FINGERPRINT no contiene una huella GPG válida." >&2
    return 6
  }

  printf '%s\n' "$GPG_PUBLIC_KEY" | gpg --batch --quiet --import
  huellas_importadas="$(gpg --batch --list-keys --with-colons | awk -F: '/^fpr:/ {print toupper($10)}')"
  printf '%s\n' "$huellas_importadas" | grep -Fxq "$huella_esperada" || {
    echo "La clave pública importada no coincide con GPG_EXPECTED_FINGERPRINT." >&2
    return 6
  }
  GPG_RECIPIENT_FINGERPRINT="$huella_esperada"

  printf '%s\n' "$GPG_SIGNING_PRIVATE_KEY" | gpg --batch --quiet --import
  huellas_privadas="$(gpg --batch --list-secret-keys --with-colons | awk -F: '/^fpr:/ {print toupper($10)}')"
  printf '%s\n' "$huellas_privadas" | grep -Fxq "$huella_firma" || {
    echo "La clave privada de firma no coincide con GPG_EXPECTED_SIGNING_FINGERPRINT." >&2
    return 6
  }
  GPG_SIGNING_FINGERPRINT="$huella_firma"
}


cifrar_archivo() {
  local origen="$1" destino="$2"
  [ -f "$origen" ] || { echo "No existe el archivo que se intentó cifrar: $origen" >&2; return 6; }
  case "$destino" in
    *.gpg) ;;
    *) echo "El destino cifrado debe terminar en .gpg: $destino" >&2; return 6 ;;
  esac

  gpg --batch --yes --quiet --trust-model always --cipher-algo AES256 --compress-algo none \
    --recipient "$GPG_RECIPIENT_FINGERPRINT" --output "$destino" --encrypt "$origen"
  [ -s "$destino" ] || { echo "GPG no generó un archivo cifrado válido: $destino" >&2; return 6; }
}

cifrar_y_firmar_archivo() {
  local origen="$1" destino="$2"
  [ -f "$origen" ] || { echo "No existe el archivo que se intentó firmar: $origen" >&2; return 6; }
  case "$destino" in
    *.gpg) ;;
    *) echo "El destino firmado y cifrado debe terminar en .gpg: $destino" >&2; return 6 ;;
  esac

  gpg --batch --yes --quiet --pinentry-mode loopback --passphrase-fd 3 --trust-model always \
    --cipher-algo AES256 --compress-algo none --local-user "$GPG_SIGNING_FINGERPRINT!" \
    --recipient "$GPG_RECIPIENT_FINGERPRINT" --output "$destino" --sign --encrypt "$origen" \
    3<<< "$GPG_SIGNING_PASSPHRASE"
  [ -s "$destino" ] || { echo "GPG no generó el archivo firmado y cifrado: $destino" >&2; return 6; }
}

firma_verificacion_preparar() {
  local huella_esperada
  huella_esperada="$(normalizar_huella_gpg "$GPG_EXPECTED_SIGNING_FINGERPRINT")"
  validar_huella_gpg "$huella_esperada" || {
    echo "GPG_EXPECTED_SIGNING_FINGERPRINT no contiene una huella GPG válida." >&2
    return 6
  }
  if [ -n "${GPG_SIGNING_PUBLIC_KEY:-}" ]; then
    printf '%s\n' "$GPG_SIGNING_PUBLIC_KEY" | gpg --batch --quiet --import
  fi
  gpg --batch --list-keys "$huella_esperada" > /dev/null 2>&1 || {
    echo "No está disponible la clave pública esperada para verificar la firma." >&2
    return 6
  }
  GPG_SIGNING_FINGERPRINT="$huella_esperada"
}

descifrar_y_verificar_firma() {
  local origen="$1" destino="$2" estado="$3" errores="$4" firmantes
  if ! gpg --batch --yes --status-fd 3 --decrypt --output "$destino" "$origen" \
      3> "$estado" 2> "$errores"; then
    echo "No se pudo descifrar o validar la firma de $origen." >&2
    return 6
  fi
  firmantes="$(awk '$1 == "[GNUPG:]" && $2 == "VALIDSIG" {print toupper($3)}' "$estado")"
  printf '%s\n' "$firmantes" | grep -Fxq "$GPG_SIGNING_FINGERPRINT" || {
    echo "Firma rechazada: $origen no fue firmado por la clave autorizada." >&2
    return 6
  }
}
