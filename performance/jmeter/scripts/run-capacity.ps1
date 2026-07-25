param(
    [int[]]$Stages = @(250, 500, 750, 1000, 1500, 2000),
    [int]$RampSeconds = 60,
    [int]$DurationSeconds = 300,
    [double]$MaxErrorPercent = 1.0,
    [int]$MaxP95Milliseconds = 1000
)

$ErrorActionPreference = 'Stop'
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runner = Join-Path $PSScriptRoot 'run-all-endpoints.ps1'
$summaryPath = Join-Path $suiteRoot ("results\capacity-summary-{0}.csv" -f (Get-Date -Format 'yyyyMMdd-HHmmss'))
$summaries = [System.Collections.Generic.List[object]]::new()

function Get-Percentile([int[]]$Values, [double]$Percentile) {
    if ($Values.Count -eq 0) { return 0 }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Max(0, [Math]::Ceiling($Percentile * $sorted.Count) - 1)
    return $sorted[$index]
}

foreach ($stage in $Stages) {
    if ($stage -lt 4) { throw "Cada etapa necesita al menos 4 usuarios." }
    Write-Host ''
    Write-Host "=== Capacidad: etapa de $stage usuarios ==="
    $stageStartedAt = Get-Date
    & $runner -Mode Load -UsersOverride $stage -RampSecondsOverride $RampSeconds -DurationSecondsOverride $DurationSeconds

    $resultDirectory = Get-ChildItem -LiteralPath (Join-Path $suiteRoot 'results') -Directory -Filter 'all-endpoints-load-*' |
        Where-Object {
            $_.CreationTime -ge $stageStartedAt.AddSeconds(-2) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'html\index.html')) -and
            (Test-Path -LiteralPath (Join-Path $_.FullName 'results.jtl'))
        } |
        Sort-Object CreationTime | Select-Object -First 1
    if (-not $resultDirectory) { throw "No se encontro el resultado de la etapa $stage." }

    $rows = @(Import-Csv -LiteralPath (Join-Path $resultDirectory.FullName 'results.jtl'))
    $endpointRows = @($rows | Where-Object { $_.label -like 'EP-*' })
    $unexpected = @($endpointRows | Where-Object { $_.success -ne 'true' })
    $serverErrors = @($endpointRows | Where-Object { $_.responseCode -match '^5\d\d$' })
    $elapsed = [int[]]@($endpointRows | ForEach-Object { [int]$_.elapsed })
    $errorPercent = if ($endpointRows.Count) { 100.0 * $unexpected.Count / $endpointRows.Count } else { 100.0 }
    $p95 = Get-Percentile $elapsed 0.95
    $p99 = Get-Percentile $elapsed 0.99
    $throughput = if ($DurationSeconds -gt 0) { $rows.Count / $DurationSeconds } else { 0 }
    $saturated = $errorPercent -ge $MaxErrorPercent -or $p95 -ge $MaxP95Milliseconds

    $summary = [pscustomobject]@{
        Users = $stage
        Samples = $rows.Count
        EndpointSamples = $endpointRows.Count
        Coverage = @($endpointRows.label | Sort-Object -Unique).Count
        UnexpectedErrors = $unexpected.Count
        ErrorPercent = [Math]::Round($errorPercent, 4)
        Http5xx = $serverErrors.Count
        AverageMs = [Math]::Round(($elapsed | Measure-Object -Average).Average, 2)
        P95Ms = $p95
        P99Ms = $p99
        MaxMs = ($elapsed | Measure-Object -Maximum).Maximum
        ThroughputPerSecond = [Math]::Round($throughput, 2)
        Saturated = $saturated
        ResultDirectory = $resultDirectory.FullName
    }
    $summaries.Add($summary)
    $summaries | Export-Csv -LiteralPath $summaryPath -NoTypeInformation -Encoding UTF8
    $summary | Format-List

    if ($saturated) {
        Write-Warning "Se alcanzo el criterio de saturacion en $stage usuarios. La ultima etapa estable es la anterior."
        break
    }
}

Write-Host ''
Write-Host "Resumen de capacidad: $summaryPath"
$summaries | Format-Table Users,Samples,ErrorPercent,Http5xx,AverageMs,P95Ms,P99Ms,ThroughputPerSecond,Saturated -AutoSize

if (-not ($summaries | Where-Object Saturated)) {
    Write-Warning "No se alcanzo saturacion. La capacidad demostrada es al menos $($summaries[-1].Users) usuarios; agrega etapas mayores para encontrar el limite."
}
