param([Parameter(Mandatory)][string]$JtlPath)

$ErrorActionPreference = 'Stop'
$rows = Import-Csv -LiteralPath $JtlPath
$auth = @($rows | Where-Object { $_.label -like 'AUTH-*' })
$endpoints = @($rows | Where-Object { $_.label -like 'EP-*' })
$uniqueEndpoints = @($endpoints.label | Sort-Object -Unique)
$http2xx = @($endpoints | Where-Object { $_.responseCode -match '^2\d\d$' })
$controlled4xx = @($endpoints | Where-Object { $_.responseCode -match '^4\d\d$' -and $_.success -eq 'true' })
$unexpected = @($endpoints | Where-Object { $_.success -ne 'true' })
$serverErrors = @($endpoints | Where-Object { $_.responseCode -match '^5\d\d$' })

Write-Host ''
Write-Host '--- Resumen de cobertura de endpoints ---'
Write-Host "Autenticaciones: $($auth.Count) | Fallidas: $(@($auth | Where-Object success -ne 'true').Count)"
Write-Host "Muestras de endpoints: $($endpoints.Count) | Endpoints unicos: $($uniqueEndpoints.Count) de 158"
Write-Host "HTTP 2xx: $($http2xx.Count) | Rechazos 4xx controlados: $($controlled4xx.Count)"
Write-Host "Fallos inesperados: $($unexpected.Count) | HTTP 5xx: $($serverErrors.Count)"

if ($unexpected.Count) {
    Write-Host ''
    Write-Host 'Fallos inesperados por endpoint:'
    $unexpected | Group-Object label,responseCode | Sort-Object Name | ForEach-Object {
        $sample = $_.Group[0]
        Write-Host "  $($sample.responseCode) x$($_.Count) - $($sample.label)"
        if ($sample.failureMessage) { Write-Host "    $($sample.failureMessage)" }
    }
}

if ($uniqueEndpoints.Count -lt 158) {
    Write-Warning "La ejecucion no cubrio los 158 endpoints. En carga aleatoria esto puede ocurrir; el smoke debe cubrirlos todos."
}
