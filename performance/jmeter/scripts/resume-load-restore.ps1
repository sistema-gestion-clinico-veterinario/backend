param(
    [string]$TargetHost = "localhost",
    [int]$TargetPort = 5432,
    [string]$TargetDatabase = "vargasvet_load",
    [string]$TargetUser = "postgres"
)

$ErrorActionPreference = "Stop"
if ($TargetDatabase -ne "vargasvet_load") {
    throw "Por seguridad, este script solo puede restaurar vargasvet_load."
}

$psql = (Get-Command psql -ErrorAction Stop).Source
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$cloneDirectory = Get-ChildItem (Join-Path $suiteRoot "results") -Directory -Filter "clone-*" |
    Where-Object { Test-Path (Join-Path $_.FullName "supabase-public.sql") } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $cloneDirectory) {
    throw "No se encontro un respaldo de Supabase para reanudar."
}

$sourceDump = Join-Path $cloneDirectory.FullName "supabase-public.sql"
$compatibleDump = Join-Path $cloneDirectory.FullName "supabase-public-compatible.sql"
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

$securePassword = Read-Host "Password del PostgreSQL local" -AsSecureString
$databasePassword = [Net.NetworkCredential]::new("", $securePassword).Password
$env:PGPASSWORD = $databasePassword

try {
    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -tAc "SELECT 1" 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo autenticar en el PostgreSQL local."
    }

    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -v ON_ERROR_STOP=1 -c "DROP SCHEMA IF EXISTS public CASCADE;" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo preparar el esquema public local."
    }

    & $psql -h $TargetHost -p $TargetPort -U $TargetUser -d $TargetDatabase -v ON_ERROR_STOP=1 -f $compatibleDump | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "La restauracion local fallo con codigo $LASTEXITCODE."
    }

    Write-Host "Restauracion reanudada y completada correctamente."
    Write-Host "Origen: $sourceDump"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $databasePassword = $null
    $securePassword = $null
}
