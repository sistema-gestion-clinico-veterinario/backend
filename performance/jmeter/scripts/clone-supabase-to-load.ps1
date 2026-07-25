param(
    [string]$SourceHost = "aws-1-us-west-2.pooler.supabase.com",
    [int]$SourcePort = 5432,
    [string]$SourceDatabase = "postgres",
    [string]$SourceUser = "postgres.toqqwxveqxhlottwetev",
    [string]$TargetHost = "localhost",
    [int]$TargetPort = 5432,
    [string]$TargetDatabase = "vargasvet_load",
    [string]$TargetUser = "postgres"
)

$ErrorActionPreference = "Stop"

if ($TargetDatabase -ne "vargasvet_load") {
    throw "Por seguridad, este script solo puede reemplazar la base local vargasvet_load."
}

$pgDump = (Get-Command pg_dump -ErrorAction Stop).Source
$psql = (Get-Command psql -ErrorAction Stop).Source
$docker = (Get-Command docker -ErrorAction Stop).Source
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $suiteRoot "results\clone-$stamp"
$sourceDump = Join-Path $output "supabase-public.sql"
$localBackup = Join-Path $output "vargasvet-load-before-clone.dump"
New-Item -ItemType Directory -Force -Path $output | Out-Null

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "SilentlyContinue"
& $docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
$dockerExitCode = $LASTEXITCODE
$ErrorActionPreference = $previousErrorActionPreference
if ($dockerExitCode -ne 0) {
    throw "Docker Desktop esta instalado, pero su motor no esta iniciado. Abre Docker Desktop y vuelve a ejecutar el script."
}

function ConvertFrom-SecureValue([Security.SecureString]$Value) {
    return [Net.NetworkCredential]::new("", $Value).Password
}

function Assert-NativeSuccess([string]$Action) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Action fallo con codigo $LASTEXITCODE."
    }
}

$sourceSecurePassword = $null
$sourcePassword = $null
$targetSecurePassword = $null
$targetPassword = $null

try {
    $env:PGSSLMODE = "require"
    $sourceAuthenticated = $false

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $sourceSecurePassword = Read-Host "Password PostgreSQL de Supabase (intento $attempt de 3)" -AsSecureString
        $sourcePassword = ConvertFrom-SecureValue $sourceSecurePassword
        $env:PGPASSWORD = $sourcePassword

        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        & $psql -h $SourceHost -p $SourcePort -U $SourceUser -d $SourceDatabase -tAc "SELECT 1" 2>$null | Out-Null
        $sourceExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference

        if ($sourceExitCode -eq 0) {
            $sourceAuthenticated = $true
            break
        }

        Write-Warning "Password incorrecto. Usa el password PostgreSQL del proyecto, no el de tu cuenta Supabase."
    }

    if (-not $sourceAuthenticated) {
        throw "No se pudo autenticar en la base Supabase despues de 3 intentos."
    }

    Write-Host "Creando copia del esquema public con PostgreSQL 17..."
    & $docker run --rm --env PGPASSWORD --env PGSSLMODE --volume "${output}:/backup" postgres:17-alpine `
        pg_dump -h $SourceHost -p $SourcePort -U $SourceUser -d $SourceDatabase `
        --schema=public --format=plain --no-owner --no-privileges --file=/backup/supabase-public.sql
    Assert-NativeSuccess "El respaldo de Supabase"

    # PostgreSQL 17 agrega esta opcion al dump, pero el destino local es PostgreSQL 16.
    $compatibleDump = Join-Path $output "supabase-public-compatible.sql"
    $reader = [IO.StreamReader]::new($sourceDump)
    $writer = [IO.StreamWriter]::new($compatibleDump, $false, [Text.UTF8Encoding]::new($false))
    try {
        while (($line = $reader.ReadLine()) -ne $null) {
            if ($line -notmatch '^SET transaction_timeout' -and
                $line -notmatch '^\\(un)?restrict\b') {
                $writer.WriteLine($line)
                if ($line -eq 'CREATE SCHEMA public;') {
                    $writer.WriteLine('CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;')
                }
            }
        }
    }
    finally {
        $reader.Dispose()
        $writer.Dispose()
    }

    $targetSecurePassword = Read-Host "Password del PostgreSQL local" -AsSecureString
    $targetPassword = ConvertFrom-SecureValue $targetSecurePassword
    $env:PGPASSWORD = $targetPassword
    Remove-Item Env:PGSSLMODE -ErrorAction SilentlyContinue

    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -tAc "SELECT 1" 2>$null | Out-Null
    Assert-NativeSuccess "La conexion a $TargetDatabase"

    Write-Host "Guardando copia recuperable de $TargetDatabase..."
    & $pgDump -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase --schema=public --format=custom --no-owner --no-privileges --file=$localBackup
    Assert-NativeSuccess "El respaldo local previo"

    Write-Host "Reemplazando solamente el esquema public de $TargetDatabase..."
    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE;" | Out-Null
    Assert-NativeSuccess "La limpieza del esquema public local"

    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -v ON_ERROR_STOP=1 -f $compatibleDump | Out-Null
    Assert-NativeSuccess "La restauracion en $TargetDatabase"

    Write-Host "Clon completado correctamente."
    Write-Host "Respaldo descargado: $sourceDump"
    Write-Host "Copia local anterior: $localBackup"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PGSSLMODE -ErrorAction SilentlyContinue
    $sourcePassword = $null
    $targetPassword = $null
    $sourceSecurePassword = $null
    $targetSecurePassword = $null
}
