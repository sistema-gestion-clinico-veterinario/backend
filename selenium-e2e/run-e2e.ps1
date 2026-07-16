[CmdletBinding()]
param(
    [switch]$Headless,
    [switch]$Video,
    [ValidateRange(0, 3000)]
    [int]$SlowMo = 150,
    [string]$Test
)

$ErrorActionPreference = 'Stop'
$moduleDir = $PSScriptRoot
$backendDir = (Resolve-Path (Join-Path $moduleDir '..')).Path
$frontendDir = (Resolve-Path (Join-Path $moduleDir '..\..\frontend')).Path
$logDir = Join-Path $moduleDir 'target\server-logs'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Test-Url([string]$Url) {
    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-Url([string]$Url, [int]$TimeoutSeconds, [string]$Name) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Url $Url) { return }
        Start-Sleep -Milliseconds 750
    }
    throw "$Name no respondió en $Url después de $TimeoutSeconds segundos. Revisa target\server-logs."
}

function Test-TcpPort([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        return $connection.Wait(500) -and $client.Connected
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-TcpPortFree([int]$Port, [int]$TimeoutSeconds = 10) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-TcpPort $Port)) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return -not (Test-TcpPort $Port)
}

function Stop-ProcessTree($Process) {
    if (-not $Process -or $Process.HasExited) { return }
    & taskkill.exe /PID $Process.Id /T /F 2>$null | Out-Null
}

foreach ($command in @('java', 'mvn', 'node', 'npm')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "No se encontró '$command' en PATH. Consulta README.md."
    }
}

$chromePaths = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
)
if (-not ($chromePaths | Where-Object { Test-Path -LiteralPath $_ })) {
    throw 'Google Chrome no está instalado. Selenium necesita Chrome o una ruta de navegador compatible.'
}

if ($Video -and -not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    throw "Se solicitó -Video, pero FFmpeg no está disponible en PATH. Instálalo o ejecuta sin -Video."
}

$backendProcess = $null
$frontendProcess = $null
$testExitCode = 1

try {
    if (-not (Wait-TcpPortFree 8080)) {
        throw 'El puerto 8080 sigue ocupado. Cierra el backend existente antes de ejecutar la suite para garantizar una base E2E limpia.'
    }
    if (-not (Wait-TcpPortFree 4200)) {
        throw 'El puerto 4200 sigue ocupado. Cierra el frontend existente antes de ejecutar la suite para garantizar que use el codigo actual.'
    }

    Push-Location $backendDir
    try {
        & mvn -q -DskipTests test-compile
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el soporte E2E del backend.' }
    } finally {
        Pop-Location
    }

    $backendProcess = Start-Process -FilePath 'mvn.cmd' -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru `
        -ArgumentList @(
            '-Dspring-boot.run.profiles=e2e',
            '-Dspring-boot.run.useTestClasspath=true',
            '-Dspring-boot.run.additional-classpath-elements=target/test-classes',
            'spring-boot:run'
        ) `
        -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') `
        -RedirectStandardError (Join-Path $logDir 'backend.err.log')
    Wait-Url 'http://127.0.0.1:8080/api/v1/setup/e2e/health' 180 'El backend E2E'

    $frontendProcess = Start-Process -FilePath 'npm.cmd' -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru `
        -ArgumentList @('run', 'start:e2e') `
        -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') `
        -RedirectStandardError (Join-Path $logDir 'frontend.err.log')
    Wait-Url 'http://127.0.0.1:4200/login' 120 'El frontend E2E'

    $mavenArgs = @(
        'test',
        "-De2e.headless=$($Headless.IsPresent.ToString().ToLowerInvariant())",
        "-De2e.video=$($Video.IsPresent.ToString().ToLowerInvariant())",
        "-De2e.slowMo=$SlowMo"
    )
    if ($Test) { $mavenArgs += "-Dtest=$Test" }

    Push-Location $moduleDir
    try {
        & mvn @mavenArgs
        $testExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    Stop-ProcessTree $frontendProcess
    Stop-ProcessTree $backendProcess
}

exit $testExitCode
