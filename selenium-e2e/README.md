# Pruebas E2E Selenium de VargasVet

Módulo independiente con Selenium WebDriver, Java 21 y JUnit 5. Reutiliza el perfil `e2e` del backend, H2 en memoria y el frontend Angular configurado con `start:e2e`.

## Documentación de QA

La documentación formal de calidad está consolidada en un solo archivo: [Documentacion_Pruebas_E2E_Selenium_VargasVet.docx](Documentacion_Pruebas_E2E_Selenium_VargasVet.docx). Incluye el plan E2E, la matriz de trazabilidad, la especificación completa de los 14 casos, el reporte de la ejecución del 14/07/2026, el índice de evidencias y las plantillas para futuras ejecuciones y defectos.

## Requisitos

- Java 21 o superior.
- Maven 3.9 o superior.
- Node.js y las dependencias del frontend instaladas con `npm install`.
- Google Chrome.
- Acceso a Internet en la primera ejecución para que Maven descargue Selenium y Selenium Manager resuelva el controlador compatible con Chrome.
- FFmpeg en `PATH` solamente si se desean videos MP4.

No es necesario descargar ChromeDriver manualmente. Selenium Manager lo administra durante la ejecución.

## Ejecución

Desde esta carpeta:

```powershell
.\run-e2e.ps1
```

Con navegador oculto:

```powershell
.\run-e2e.ps1 -Headless
```

Con videos y 500 ms entre acciones:

```powershell
.\run-e2e.ps1 -Video -SlowMo 500
```

Ejecutar una clase o caso específico:

```powershell
.\run-e2e.ps1 -Test "AppointmentBusinessE2ETest"
.\run-e2e.ps1 -Test "AppointmentBusinessE2ETest#caso01_*"
```

El script levanta backend y frontend cuando no están activos y cierra únicamente los procesos que él mismo inició.

## Evidencias

- Capturas: `target/evidence/screenshots/`
- Videos: `target/evidence/videos/`
- Resultados JUnit: `target/surefire-reports/`
- Logs de servidores: `target/server-logs/`

Las capturas se generan para todos los casos. Los videos requieren ejecutar con `-Video` y tener FFmpeg disponible.

## Casos implementados

| ID | Regla de negocio |
|---|---|
| 01 | Reserva concurrente del mismo horario |
| 02 | Disponibilidad por clínica, turno, duración y ocupación |
| 03 | Límites diarios del apoderado |
| 04 | Inicio controlado de atención clínica |
| 13 | Desactivación y reactivación de empresa |
| 14 | Desactivación de rol asignado |
| 16 | Reprogramación con conservación de datos |
| 18 | Emergencia con clínica cerrada |
| 21 | Empleado inactivo fuera de disponibilidad |
| 27 | Control de versión en edición clínica concurrente |
| 28 | Servicio inactivo sin perder historial |
| 31 | Activación de cuenta de empleado |
| 24 | Aislamiento de datos del apoderado |
| 40 | Historia clínica, radiografía y asistencia IA |

Los escenarios que alteran empresa, rol, empleado, servicio u horario operativo restauran el estado en bloques `finally` para no contaminar los casos posteriores.
