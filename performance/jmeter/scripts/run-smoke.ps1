param(
    [string]$PropertiesFile = ""
)

$ErrorActionPreference = "Stop"
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$repoRoot = (Resolve-Path (Join-Path $suiteRoot "..\..")).Path
$workshopRoot = Split-Path $repoRoot -Parent
$jmeter = Join-Path $workshopRoot "tools\apache-jmeter-5.6.3\bin\jmeter.bat"
$plan = Join-Path $suiteRoot "plans\vargasvet-smoke.jmx"

if (-not $PropertiesFile) {
    $PropertiesFile = Join-Path $suiteRoot "config\load-test.local.properties"
}
if (-not (Test-Path -LiteralPath $jmeter)) { throw "No se encontró JMeter en $jmeter" }
if (-not (Test-Path -LiteralPath $PropertiesFile)) { throw "Crea $PropertiesFile desde load-test.example.properties" }
. (Join-Path $PSScriptRoot "ensure-load-credentials.ps1")

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $suiteRoot "results\smoke-$stamp"
$report = Join-Path $output "html"
New-Item -ItemType Directory -Force -Path $output | Out-Null

$jtl = Join-Path $output "results.jtl"
& $jmeter -n -t $plan -q $PropertiesFile -Jusers=1 -Jramp_seconds=1 -Jduration_seconds=30 -Jloops=1 -l $jtl -j (Join-Path $output "jmeter.log") -e -o $report
if ($LASTEXITCODE -ne 0) { throw "La prueba de humo falló. Revisa $output" }
& (Join-Path $PSScriptRoot "evaluate-results.ps1") -ResultFile $jtl
if ($LASTEXITCODE -ne 0) { throw "La prueba no cumplió los criterios de aceptación. Revisa $output" }
Write-Host "Prueba de humo terminada: $report\index.html"
