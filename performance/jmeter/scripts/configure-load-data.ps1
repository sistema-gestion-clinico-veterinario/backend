param(
    [string]$DatabaseHost = "localhost",
    [int]$DatabasePort = 5432,
    [string]$DatabaseName = "vargasvet_load",
    [string]$DatabaseUser = "postgres"
)

$ErrorActionPreference = "Stop"
$psql = (Get-Command psql -ErrorAction Stop).Source
$suiteRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$propertiesFile = Join-Path $suiteRoot "config\load-test.local.properties"

$email = (Read-Host "Correo del administrador activo de la copia").Trim().ToLowerInvariant()
if ($email -notmatch '^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$') {
    throw "El correo no tiene un formato valido."
}
$escapedEmail = $email.Replace("'", "''")
$ownerEmail = (Read-Host "Correo del apoderado usado en la carga").Trim().ToLowerInvariant()
if ($ownerEmail -notmatch '^[a-z0-9._%+\-]+@[a-z0-9.\-]+\.[a-z]{2,}$') {
    throw "El correo del apoderado no tiene un formato valido."
}
$escapedOwnerEmail = $ownerEmail.Replace("'", "''")

if ($DatabaseName -notmatch '(?i)load') {
    throw "Por seguridad, este script solo puede crear fixtures en una base cuyo nombre contenga 'load'. Base recibida: $DatabaseName"
}

$securePassword = Read-Host "Password del PostgreSQL local" -AsSecureString
$databasePassword = [Net.NetworkCredential]::new("", $securePassword).Password
$env:PGPASSWORD = $databasePassword

$fixtureUuid = [Guid]::NewGuid().ToString()
$fixtureMedicalRecord = "LT-" + [Guid]::NewGuid().ToString('N').Substring(0, 12).ToUpperInvariant()
$fixtureSql = @"
WITH target_guardian AS (
    SELECT a.id
    FROM apoderado a
    JOIN usuario u ON u.id = a.user_id
    WHERE lower(u.email) = lower('$escapedOwnerEmail')
    LIMIT 1
), inserted_pet AS (
    INSERT INTO mascota (
        nombre_completo, especie, esterilizado, activo, apoderado_id, uuid, created_at, updated_at
    )
    SELECT 'Mascota JMeter', 'PERRO', false, true, tg.id, '$fixtureUuid', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM target_guardian tg
    WHERE NOT EXISTS (
        SELECT 1 FROM mascota m WHERE m.apoderado_id = tg.id AND m.activo
    )
    RETURNING id
), selected_fixture_pet AS (
    SELECT id FROM inserted_pet
    UNION ALL
    SELECT m.id
    FROM mascota m, target_guardian tg
    WHERE m.apoderado_id = tg.id AND m.activo
    ORDER BY id
    LIMIT 1
)
INSERT INTO historia_clinica (numero_hc, mascota_id, activa, created_at, updated_at)
SELECT '$fixtureMedicalRecord', sfp.id, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM selected_fixture_pet sfp
WHERE NOT EXISTS (SELECT 1 FROM historia_clinica hc WHERE hc.mascota_id = sfp.id);
"@

& $psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -v ON_ERROR_STOP=1 -c $fixtureSql 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "No se pudo preparar la mascota e historia clinica del apoderado en $DatabaseName."
}

$sql = @"
WITH selected_company AS (
    SELECT COALESCE(
        (SELECT company_id FROM usuario WHERE lower(email) = lower('$escapedOwnerEmail') AND activo LIMIT 1),
        (SELECT company_id FROM usuario WHERE lower(email) = lower('$escapedEmail') AND activo LIMIT 1),
        (SELECT id FROM company WHERE activo ORDER BY id LIMIT 1)
    ) AS id
), selected_pair AS (
    SELECT e.id AS employee_id, s.id AS service_id
    FROM empleado_servicio es
    JOIN empleado e ON e.id = es.empleado_id AND e.estado
    JOIN usuario eu ON eu.id = e.user_id AND eu.activo
    JOIN servicios s ON s.id = es.servicio_id AND s.activo AND s.disponible
    JOIN selected_company sc ON sc.id = eu.company_id AND sc.id = s.company_id
    ORDER BY e.id, s.id
    LIMIT 1
), selected_employee AS (
    SELECT COALESCE(
        (SELECT employee_id FROM selected_pair),
        (SELECT e.id FROM empleado e JOIN usuario u ON u.id=e.user_id, selected_company sc
         WHERE u.company_id=sc.id AND e.estado ORDER BY e.id LIMIT 1)
    ) AS id
), selected_service AS (
    SELECT COALESCE(
        (SELECT service_id FROM selected_pair),
        (SELECT s.id FROM servicios s, selected_company sc
         WHERE s.company_id=sc.id AND s.activo AND s.disponible ORDER BY s.id LIMIT 1)
    ) AS id
), selected_pet AS (
    SELECT m.id
    FROM mascota m
    JOIN apoderado a ON a.id=m.apoderado_id
    JOIN usuario u ON u.id=a.user_id
    JOIN selected_company sc ON sc.id=u.company_id
    WHERE m.activo AND lower(u.email) = lower('$escapedOwnerEmail')
    ORDER BY m.id
    LIMIT 1
), selected_consultation AS (
    SELECT c.id
    FROM consulta c
    JOIN historia_clinica h ON h.id=c.historia_clinica_id
    JOIN mascota m ON m.id=h.mascota_id
    JOIN apoderado a ON a.id=m.apoderado_id
    JOIN usuario u ON u.id=a.user_id
    JOIN selected_company sc ON sc.id=u.company_id
    ORDER BY c.id DESC
    LIMIT 1
), selected_appointment AS (
    SELECT c.id
    FROM citas c
    JOIN mascota m ON m.id=c.mascota_id
    JOIN apoderado a ON a.id=m.apoderado_id
    JOIN usuario u ON u.id=a.user_id
    JOIN selected_company sc ON sc.id=u.company_id
    WHERE NOT c.eliminada
    ORDER BY c.id DESC
    LIMIT 1
)
SELECT
    COALESCE((SELECT id FROM selected_company),0),
    COALESCE((SELECT id FROM selected_pet),0),
    COALESCE((SELECT id FROM selected_employee),0),
    COALESCE((SELECT id FROM selected_service),0),
    COALESCE((SELECT fecha FROM horario_empleado h, selected_employee e
              WHERE h.empleado_id=e.id AND h.activo AND h.fecha >= CURRENT_DATE
              ORDER BY h.fecha LIMIT 1), CURRENT_DATE + 1),
    COALESCE((SELECT id FROM selected_consultation),0),
    COALESCE((SELECT id FROM selected_appointment),0),
    COALESCE((SELECT id FROM usuario WHERE lower(email)=lower('$escapedEmail') LIMIT 1),0),
    COALESCE((SELECT a.id FROM apoderado a JOIN usuario u ON u.id=a.user_id WHERE lower(u.email)=lower('$escapedOwnerEmail') LIMIT 1),0),
    COALESCE((SELECT uuid FROM mascota WHERE id=(SELECT id FROM selected_pet)),''),
    COALESCE((SELECT id FROM historia_clinica WHERE mascota_id=(SELECT id FROM selected_pet)),0),
    COALESCE((SELECT numero_hc FROM historia_clinica WHERE mascota_id=(SELECT id FROM selected_pet)),''),
    COALESCE((SELECT id FROM roles WHERE activo ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM usuario_por_rol WHERE usuario_id=(SELECT id FROM usuario WHERE lower(email)=lower('$escapedEmail') LIMIT 1) ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM horario_empleado WHERE empleado_id=(SELECT id FROM selected_employee) ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM tipo_empleado ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM especialidad ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM ventanas ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM vistas ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM prescripciones ORDER BY id LIMIT 1),0),
    COALESCE((SELECT id FROM archivos_clinicos ORDER BY id LIMIT 1),0);
"@

try {
    $result = & $psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -tA -F '|' -c $sql 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $result) {
        throw "No se pudieron consultar los datos clonados."
    }

    $values = $result.Trim().Split('|')
    if ($values.Count -ne 21) {
        throw "La consulta devolvio un formato inesperado."
    }
    $missing = @()
    if ([int64]$values[0] -eq 0) { $missing += 'empresa' }
    if ([int64]$values[1] -eq 0) { $missing += 'mascota activa del apoderado' }
    if ([int64]$values[2] -eq 0) { $missing += 'empleado activo' }
    if ([int64]$values[3] -eq 0) { $missing += 'servicio activo' }
    if ($missing.Count -gt 0) {
        throw "Faltan datos para JMeter en la empresa del apoderado: $($missing -join ', ')."
    }

    $lines = @(
        'protocol=http',
        'host=127.0.0.1',
        'port=8081',
        'base_path=/api/v1',
        "company_id=$($values[0])",
        "pet_id=$($values[1])",
        "employee_id=$($values[2])",
        "service_id=$($values[3])",
        "appointment_date=$($values[4])",
        "consultation_id=$($values[5])",
        "payment_appointment_id=$($values[6])",
        "appointment_id=$($values[6])",
        "user_id=$($values[7])",
        "guardian_id=$($values[8])",
        "pet_uuid=$($values[9])",
        "medical_record_id=$($values[10])",
        "medical_record_number=$($values[11])",
        "role_id=$($values[12])",
        "user_role_id=$($values[13])",
        "schedule_id=$($values[14])",
        "employee_type_id=$($values[15])",
        "specialty_id=$($values[16])",
        "window_id=$($values[17])",
        "view_id=$($values[18])",
        "prescription_id=$($values[19])",
        "file_id=$($values[20])",
        'page_size=10',
        'think_time_ms=50',
        'think_time_range_ms=50'
    )
    [IO.File]::WriteAllLines($propertiesFile, $lines, [Text.UTF8Encoding]::new($false))

    Write-Host "Configuracion de JMeter actualizada: $propertiesFile"
    Write-Host "company_id=$($values[0]), pet_id=$($values[1]), employee_id=$($values[2]), service_id=$($values[3])"
    Write-Host "consultation_id=$($values[5]), payment_appointment_id=$($values[6])"
}
finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    $databasePassword = $null
    $securePassword = $null
}
