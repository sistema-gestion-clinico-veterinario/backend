param(
    [string]$PropertiesFile = "",
    [string]$TransactionCsv = ""
)

$ErrorActionPreference = "Stop"
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$repoRoot = (Resolve-Path (Join-Path $suiteRoot "..\..")).Path
$workshopRoot = Split-Path $repoRoot -Parent
$jmeter = Join-Path $workshopRoot "tools\apache-jmeter-5.6.3\bin\jmeter.bat"
$plan = Join-Path $suiteRoot "plans\vargasvet-transaction-load.jmx"
if (-not $PropertiesFile) { $PropertiesFile = Join-Path $suiteRoot "config\load-test.local.properties" }
if (-not $TransactionCsv) { $TransactionCsv = Join-Path $suiteRoot "config\transactions.local.csv" }
if (-not (Test-Path -LiteralPath $PropertiesFile)) { throw "No existe $PropertiesFile" }
if (-not (Test-Path -LiteralPath $TransactionCsv)) { throw "Crea $TransactionCsv con datos desechables" }
. (Join-Path $PSScriptRoot "ensure-load-credentials.ps1")

$rows = @(Import-Csv -LiteralPath $TransactionCsv)
if ($rows.Count -eq 0) { throw "El CSV transaccional no contiene operaciones." }
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$output = Join-Path $suiteRoot "results\transactions-$stamp"
$report = Join-Path $output "html"
New-Item -ItemType Directory -Force -Path $output | Out-Null

$jtl = Join-Path $output "results.jtl"
& $jmeter -n -t $plan -q $PropertiesFile -Jusers=$($rows.Count) -Jramp_seconds=10 -Jduration_seconds=120 -Jloops=1 -Jtransaction_csv=$TransactionCsv -l $jtl -j (Join-Path $output "jmeter.log") -e -o $report
if ($LASTEXITCODE -ne 0) { throw "La prueba transaccional falló. Revisa $output" }
& (Join-Path $PSScriptRoot "evaluate-results.ps1") -ResultFile $jtl
if ($LASTEXITCODE -ne 0) { throw "La prueba no cumplió los criterios de aceptación. Revisa $output" }
Write-Host "Prueba transaccional terminada: $report\index.html"
