if (-not $env:LOAD_TEST_EMAIL) {
    $env:LOAD_TEST_EMAIL = (Read-Host "Correo del usuario de la aplicacion").Trim().ToLowerInvariant()
}

if (-not $env:LOAD_TEST_PASSWORD) {
    $secureLoadPassword = Read-Host "Password del usuario de la aplicacion" -AsSecureString
    $env:LOAD_TEST_PASSWORD = [Net.NetworkCredential]::new("", $secureLoadPassword).Password
    $secureLoadPassword = $null
}

if (-not $env:LOAD_TEST_EMAIL -or -not $env:LOAD_TEST_PASSWORD) {
    throw "Las credenciales de carga son obligatorias."
}
