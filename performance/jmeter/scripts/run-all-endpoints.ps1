param(
    [ValidateSet("Smoke", "Load")]
    [string]$Mode = "Smoke",
    [string]$PropertiesFile = "",
    [switch]$ResetCredentials,
    [int]$UsersOverride = 0,
    [int]$RampSecondsOverride = 0,
    [int]$DurationSecondsOverride = 0
)

$ErrorActionPreference = "Stop"
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$repoRoot = (Resolve-Path (Join-Path $suiteRoot "..\..")).Path
$workshopRoot = Split-Path $repoRoot -Parent
$jmeter = Join-Path $workshopRoot "tools\apache-jmeter-5.6.3\bin\jmeter.bat"
$sampleFile = Join-Path $workshopRoot "qa_documentacion_carga\evidencias\contact-sheet.png"
$credentialFile = Join-Path $suiteRoot "config\load-test-credentials.local.xml"

if (-not $PropertiesFile) {
    $PropertiesFile = Join-Path $suiteRoot "config\load-test.local.properties"
}
if (-not (Test-Path -LiteralPath $jmeter)) { throw "No se encontro JMeter en $jmeter" }
if (-not (Test-Path -LiteralPath $PropertiesFile)) { throw "No existe $PropertiesFile" }
if (-not (Test-Path -LiteralPath $sampleFile)) { throw "No existe el archivo de muestra $sampleFile" }

function Read-Secret([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    try { return [Net.NetworkCredential]::new("", $secure).Password }
    finally { $secure = $null }
}

if ($ResetCredentials) {
    Remove-Item -LiteralPath $credentialFile -Force -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_OWNER_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_OWNER_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_VET_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_VET_PASSWORD -ErrorAction SilentlyContinue
}

if ((Test-Path -LiteralPath $credentialFile) -and -not $env:LOAD_TEST_EMAIL) {
    try {
        $saved = Import-Clixml -LiteralPath $credentialFile
        $env:LOAD_TEST_EMAIL = $saved.AdminEmail
        $env:LOAD_TEST_PASSWORD = [Net.NetworkCredential]::new('', $saved.AdminPassword).Password
        $env:LOAD_TEST_OWNER_EMAIL = $saved.OwnerEmail
        $env:LOAD_TEST_OWNER_PASSWORD = [Net.NetworkCredential]::new('', $saved.OwnerPassword).Password
        $env:LOAD_TEST_VET_EMAIL = $saved.VetEmail
        $env:LOAD_TEST_VET_PASSWORD = [Net.NetworkCredential]::new('', $saved.VetPassword).Password
        Write-Host "Credenciales cifradas cargadas desde $credentialFile"
    }
    catch {
        Remove-Item -LiteralPath $credentialFile -Force -ErrorAction SilentlyContinue
        Write-Warning "No se pudieron leer las credenciales cifradas; se solicitaran nuevamente."
    }
}

if (-not $env:LOAD_TEST_EMAIL) {
    $env:LOAD_TEST_EMAIL = (Read-Host "Correo del administrador/superadministrador de carga").Trim().ToLowerInvariant()
    $env:LOAD_TEST_PASSWORD = Read-Secret "Password del administrador"
}
if (-not $env:LOAD_TEST_OWNER_EMAIL) {
    $ownerEmail = (Read-Host "Correo de apoderado (Enter para reutilizar administrador)").Trim().ToLowerInvariant()
    if ($ownerEmail) {
        $env:LOAD_TEST_OWNER_EMAIL = $ownerEmail
        $env:LOAD_TEST_OWNER_PASSWORD = Read-Secret "Password del apoderado"
    } else {
        $env:LOAD_TEST_OWNER_EMAIL = $env:LOAD_TEST_EMAIL
        $env:LOAD_TEST_OWNER_PASSWORD = $env:LOAD_TEST_PASSWORD
    }
}
if (-not $env:LOAD_TEST_VET_EMAIL) {
    $vetEmail = (Read-Host "Correo de veterinario (Enter para reutilizar administrador)").Trim().ToLowerInvariant()
    if ($vetEmail) {
        $env:LOAD_TEST_VET_EMAIL = $vetEmail
        $env:LOAD_TEST_VET_PASSWORD = Read-Secret "Password del veterinario"
    } else {
        $env:LOAD_TEST_VET_EMAIL = $env:LOAD_TEST_EMAIL
        $env:LOAD_TEST_VET_PASSWORD = $env:LOAD_TEST_PASSWORD
    }
}

function Set-LocalLoadPassword([string]$Email, [string]$NewPassword) {
    $adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $adminBody = @{ email = $env:LOAD_TEST_EMAIL; password = $env:LOAD_TEST_PASSWORD } | ConvertTo-Json -Compress
    Invoke-WebRequest -UseBasicParsing -Method Post `
        -Uri 'http://127.0.0.1:8081/api/v1/auth/login' `
        -ContentType 'application/json' -Body $adminBody -WebSession $adminSession -TimeoutSec 15 | Out-Null

    $resetBody = @{ email = $Email; newPassword = $NewPassword } | ConvertTo-Json -Compress
    Invoke-WebRequest -UseBasicParsing -Method Post `
        -Uri 'http://127.0.0.1:8081/api/v1/admin/users/reset-password' `
        -ContentType 'application/json' -Body $resetBody -WebSession $adminSession -TimeoutSec 15 | Out-Null
}

function Assert-Login([string]$Label, [string]$Email, [string]$Password, [switch]$AllowLocalReset) {
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json -Compress
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Method Post `
            -Uri 'http://127.0.0.1:8081/api/v1/auth/login' `
            -ContentType 'application/json' -Body $body -TimeoutSec 15
        if ($response.StatusCode -ne 200) { throw "HTTP $($response.StatusCode)" }
        Write-Host "Credencial $Label validada."
    }
    catch {
        if ($AllowLocalReset) {
            $answer = (Read-Host "La cuenta $Label existe en otra version de la base. Restablecer su password SOLO en vargasvet_load? [S/N]").Trim()
            if ($answer -match '^(s|si|sí|y|yes)$') {
                try {
                    Set-LocalLoadPassword $Email $Password
                    $retry = Invoke-WebRequest -UseBasicParsing -Method Post `
                        -Uri 'http://127.0.0.1:8081/api/v1/auth/login' `
                        -ContentType 'application/json' -Body $body -TimeoutSec 15
                    if ($retry.StatusCode -eq 200) {
                        Write-Host "Credencial $Label sincronizada y validada en vargasvet_load."
                        return
                    }
                }
                catch {
                    Write-Warning "No se pudo sincronizar la cuenta $Label en la copia local: $($_.Exception.Message)"
                }
            }
        }
        if ($Label -eq 'administrador') {
            Remove-Item Env:LOAD_TEST_EMAIL -ErrorAction SilentlyContinue
            Remove-Item Env:LOAD_TEST_PASSWORD -ErrorAction SilentlyContinue
        }
        elseif ($Label -eq 'apoderado') {
            Remove-Item Env:LOAD_TEST_OWNER_EMAIL -ErrorAction SilentlyContinue
            Remove-Item Env:LOAD_TEST_OWNER_PASSWORD -ErrorAction SilentlyContinue
        }
        elseif ($Label -eq 'veterinario') {
            Remove-Item Env:LOAD_TEST_VET_EMAIL -ErrorAction SilentlyContinue
            Remove-Item Env:LOAD_TEST_VET_PASSWORD -ErrorAction SilentlyContinue
        }
        Remove-Item -LiteralPath $credentialFile -Force -ErrorAction SilentlyContinue
        throw "Las credenciales de $Label no fueron aceptadas por /auth/login. Verifica correo, password, estado y empresa activa."
    }
}

Assert-Login 'administrador' $env:LOAD_TEST_EMAIL $env:LOAD_TEST_PASSWORD
Assert-Login 'apoderado' $env:LOAD_TEST_OWNER_EMAIL $env:LOAD_TEST_OWNER_PASSWORD -AllowLocalReset
Assert-Login 'veterinario' $env:LOAD_TEST_VET_EMAIL $env:LOAD_TEST_VET_PASSWORD -AllowLocalReset

if (-not (Test-Path -LiteralPath $credentialFile)) {
    [PSCustomObject]@{
        AdminEmail = $env:LOAD_TEST_EMAIL
        AdminPassword = ConvertTo-SecureString $env:LOAD_TEST_PASSWORD -AsPlainText -Force
        OwnerEmail = $env:LOAD_TEST_OWNER_EMAIL
        OwnerPassword = ConvertTo-SecureString $env:LOAD_TEST_OWNER_PASSWORD -AsPlainText -Force
        VetEmail = $env:LOAD_TEST_VET_EMAIL
        VetPassword = ConvertTo-SecureString $env:LOAD_TEST_VET_PASSWORD -AsPlainText -Force
    } | Export-Clixml -LiteralPath $credentialFile
    Write-Host "Credenciales guardadas cifradas para este usuario de Windows."
}

if ($Mode -eq "Smoke") {
    $plan = Join-Path $suiteRoot "plans\vargasvet-all-endpoints-smoke.jmx"
    $users = 4
    $adminUsers = 1
    $ownerUsers = 1
    $vetUsers = 1
    $authUsers = 1
    $ramp = 1
    $duration = 600
    $loops = 1
    $thinkTime = 0
    $thinkTimeRange = 0
} else {
    $plan = Join-Path $suiteRoot "plans\vargasvet-all-endpoints-load.jmx"
    $users = if ($UsersOverride -gt 0) { $UsersOverride } else { 250 }
    $authUsers = [Math]::Max(1, [Math]::Round($users * 0.02))
    $ownerUsers = [Math]::Max(1, [Math]::Round($users * 0.20))
    $vetUsers = [Math]::Max(1, [Math]::Round($users * 0.20))
    $adminUsers = $users - $authUsers - $ownerUsers - $vetUsers
    if ($adminUsers -lt 1) { throw "La cantidad total de usuarios debe permitir al menos un usuario por grupo." }
    $ramp = if ($RampSecondsOverride -gt 0) { $RampSecondsOverride } else { 300 }
    $duration = if ($DurationSecondsOverride -gt 0) { $DurationSecondsOverride } else { 900 }
    $loops = -1
    $thinkTime = 3000
    $thinkTimeRange = 5000
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $suiteRoot "results\all-endpoints-$($Mode.ToLowerInvariant())-$stamp"
$report = Join-Path $output "html"
$jtl = Join-Path $output "results.jtl"
New-Item -ItemType Directory -Force -Path $output | Out-Null
$runtimeProperties = Join-Path ([IO.Path]::GetTempPath()) ("vargasvet-jmeter-" + [Guid]::NewGuid().ToString('N') + ".properties")

function ConvertTo-JavaPropertyValue([string]$Value) {
    return $Value.Replace('\', '\\').Replace("`r", '\r').Replace("`n", '\n').Replace('=', '\=').Replace(':', '\:')
}

$propertyLines = @(
    Get-Content -LiteralPath $PropertiesFile
    "load_email=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_EMAIL)"
    "load_password=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_PASSWORD)"
    "load_owner_email=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_OWNER_EMAIL)"
    "load_owner_password=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_OWNER_PASSWORD)"
    "load_vet_email=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_VET_EMAIL)"
    "load_vet_password=$(ConvertTo-JavaPropertyValue $env:LOAD_TEST_VET_PASSWORD)"
)
[IO.File]::WriteAllLines($runtimeProperties, $propertyLines, [Text.UTF8Encoding]::new($false))

try {
    Write-Host "Ejecutando Apache JMeter: $Mode"
    Write-Host "Plan: $plan"
    Write-Host "Usuarios: $users (admin=$adminUsers, apoderado=$ownerUsers, veterinario=$vetUsers, auth=$authUsers) | Ramp-up: $ramp s | Duracion maxima: $duration s"
    $jmeterArguments = @(
        '-n',
        '-t', $plan,
        '-q', $runtimeProperties,
        "-Jusers=$users",
        "-Jadmin_users=$adminUsers",
        "-Jowner_users=$ownerUsers",
        "-Jvet_users=$vetUsers",
        "-Jauth_users=$authUsers",
        "-Jramp_seconds=$ramp",
        "-Jduration_seconds=$duration",
        "-Jloops=$loops",
        "-Jthink_time_ms=$thinkTime",
        "-Jthink_time_range_ms=$thinkTimeRange",
        "-Jsample_file=$sampleFile",
        '-l', $jtl,
        '-j', (Join-Path $output 'jmeter.log'),
        '-e',
        '-o', $report
    )
    & $jmeter @jmeterArguments
    if ($LASTEXITCODE -ne 0) { throw "JMeter termino con codigo $LASTEXITCODE. Revisa $output" }
    & (Join-Path $PSScriptRoot 'analyze-all-endpoints.ps1') -JtlPath $jtl
    Write-Host "Resultado JTL: $jtl"
    Write-Host "Dashboard: $(Join-Path $report 'index.html')"
}
finally {
    Remove-Item -LiteralPath $runtimeProperties -Force -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_OWNER_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_OWNER_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_VET_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:LOAD_TEST_VET_PASSWORD -ErrorAction SilentlyContinue
}
