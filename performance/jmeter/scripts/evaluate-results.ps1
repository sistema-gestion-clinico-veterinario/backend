param(
    [Parameter(Mandatory = $true)]
    [string]$ResultFile,
    [double]$MaximumErrorPercent = 1.0,
    [int]$MaximumP95Milliseconds = 2000
)
$ErrorActionPreference = "Stop"
if (-not (Test-Path -LiteralPath $ResultFile)) { throw "No existe el archivo JTL: $ResultFile" }

$samples = @(Import-Csv -LiteralPath $ResultFile)
if ($samples.Count -eq 0) { throw "El archivo JTL no contiene muestras." }
$errors = @($samples | Where-Object { $_.success -ne "true" })
$http500 = @($samples | Where-Object { $_.responseCode -match '^5\d\d$' })
$elapsed = @($samples | ForEach-Object { [int]$_.elapsed } | Sort-Object)
$p95Index = [Math]::Max(0, [Math]::Ceiling($elapsed.Count * 0.95) - 1)
$p95 = $elapsed[$p95Index]
$average = [Math]::Round(($elapsed | Measure-Object -Average).Average, 2)
$errorPercent = [Math]::Round(($errors.Count * 100.0) / $samples.Count, 2)
$passed = $errorPercent -lt $MaximumErrorPercent -and $p95 -lt $MaximumP95Milliseconds -and $http500.Count -eq 0
$summary = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    totalSamples = $samples.Count
    failedSamples = $errors.Count
    errorPercent = $errorPercent
    averageMilliseconds = $average
    p95Milliseconds = $p95
    http500Responses = $http500.Count
    maximumErrorPercent = $MaximumErrorPercent
    maximumP95Milliseconds = $MaximumP95Milliseconds
    status = if ($passed) { "APROBADO" } else { "NO_APROBADO" }
}
$summaryPath = Join-Path (Split-Path $ResultFile -Parent) "acceptance-summary.json"
$summary | ConvertTo-Json | Set-Content -LiteralPath $summaryPath -Encoding UTF8
$summary.GetEnumerator() | ForEach-Object { Write-Host "$($_.Key): $($_.Value)" }
Write-Host "Resumen: $summaryPath"

if (-not $passed) { exit 2 }

