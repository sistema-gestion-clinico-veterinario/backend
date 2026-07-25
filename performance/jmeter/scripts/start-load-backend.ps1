param(
    [string]$DatabaseName = "vargasvet_load",
    [string]$DatabaseHost = "localhost",
    [int]$DatabasePort = 5432,
    [string]$DatabaseUser = "postgres",
    [int]$ServerPort = 8081
)
$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$psql = (Get-Command psql -ErrorAction Stop).Source
$securePassword = $null
$databasePassword = $null
$passwordIsValid = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    $securePassword = Read-Host "Password de PostgreSQL para $DatabaseUser (intento $attempt de 3)" -AsSecureString
    $databasePassword = [Net.NetworkCredential]::new("", $securePassword).Password
    $env:PGPASSWORD = $databasePassword

    & $psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -tAc "SELECT 1" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $passwordIsValid = $true
        break
    }

    Write-Warning "Password incorrecto para PostgreSQL. Intenta nuevamente."
}
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
if (-not $passwordIsValid) {
    throw "No se pudo autenticar en PostgreSQL despues de 3 intentos."
}
$env:DATABASE_URL = "jdbc:postgresql://${DatabaseHost}:${DatabasePort}/${DatabaseName}"
$env:DB_USERNAME = $DatabaseUser
$env:DB_PASSWORD = $databasePassword
$env:CORS_ALLOWED_ORIGINS = "http://127.0.0.1:4200,http://localhost:4200"
$env:COOKIE_SECURE = "false"
$env:COOKIE_SAME_SITE = "Lax"
$env:SPRING_PROFILES_ACTIVE = "load"
$env:SERVER_PORT = $ServerPort
$env:APP_RATE_LIMIT_ENABLED = "false"
$mockScript = Join-Path $repoRoot "performance\jmeter\tools\mock_external_services.mjs"
$node = (Get-Command node -ErrorAction Stop).Source
$mockProcess = Start-Process -FilePath $node -ArgumentList @($mockScript) -PassThru -WindowStyle Hidden
$env:SUPABASE_URL = "http://127.0.0.1:8099"
$env:SUPABASE_SERVICE_ROLE_KEY = "load-test-not-used"
$env:SUPABASE_BUCKET = "load-test"
$env:IA_LABORATORIO_URL = "http://127.0.0.1:8099"
$env:IA_RADIOGRAFIA_URL = "http://127.0.0.1:8099"
$env:MAIL_USERNAME = "load-test-not-used"
$env:MAIL_PASSWORD = "load-test-not-used"
try {
    Write-Host "Iniciando backend de carga en http://127.0.0.1:$ServerPort/api/v1"
    Push-Location $repoRoot
    & mvn spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "El backend termino con codigo $LASTEXITCODE."
    }
}
finally {
    Pop-Location
    if ($mockProcess -and -not $mockProcess.HasExited) {
        Stop-Process -Id $mockProcess.Id -Force -ErrorAction SilentlyContinue
    }
    $databasePassword = $null
    $securePassword = $null
}
