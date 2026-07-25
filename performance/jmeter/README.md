# Pruebas de carga de VargasVet con Apache JMeter

Esta suite contiene pruebas no funcionales de rendimiento contra la API REST de VargasVet. No debe ejecutarse contra producción.

## Cobertura

- PC-001: inicio de sesión.
- PC-002: renovación del token.
- PC-003: estadísticas del dashboard.
- PC-004: listado de mascotas.
- PC-005: detalle de mascota.
- PC-006: listado de citas.
- PC-007: disponibilidad de citas.
- PC-008: servicios de una mascota.
- PC-009: listado de historias clínicas.
- PC-010: historia clínica de una mascota.
- PC-011: consulta clínica por ID; se omite cuando `consultation_id=0`.
- PC-012: controles preventivos.
- PC-013: historial de pagos.
- PC-014: registro de cita mediante CSV transaccional.
- PC-015: reprogramación mediante CSV transaccional.
- PC-016: cancelación mediante CSV transaccional.
- PC-017: registro de pago mediante CSV transaccional.

## Preparación

1. Levantar el backend con una base de datos exclusiva de pruebas.
2. Copiar `config/load-test.example.properties` como `config/load-test.local.properties`.
3. Reemplazar los identificadores con registros existentes en la base de pruebas.
4. Definir las credenciales en PowerShell:

```powershell
$env:LOAD_TEST_EMAIL="usuario-pruebas@vargasvet.test"
$env:LOAD_TEST_PASSWORD="contraseña-de-pruebas"
```

## Ejecución

Validación con un usuario y una sola iteración:

```powershell
.\performance\jmeter\scripts\run-smoke.ps1
```

La prueba de humo recorre PC-001 a PC-013 una vez. En la carga de 200 usuarios, cada iteración selecciona una operación mediante una distribución ponderada: dashboard 10 %, mascotas 20 %, citas y disponibilidad 30 %, historias y consultas clínicas 15 %, controles preventivos 10 % y pagos 15 %.

Carga progresiva: 200 usuarios, 5 minutos de incremento y 15 minutos totales:

```powershell
.\performance\jmeter\scripts\run-load-200.ps1
```

Transacciones controladas: copiar `transactions.example.csv` como `transactions.local.csv`, reemplazar todos los identificadores y ejecutar:

```powershell
.\performance\jmeter\scripts\run-transactions.ps1
```

Cada ejecución crea `results.jtl`, `jmeter.log`, `acceptance-summary.json` y un reporte HTML dentro de una carpeta con fecha y hora. La carga debe ejecutarse en modo CLI; la interfaz gráfica de JMeter se reserva para inspeccionar o editar el plan.

## Criterios de aceptación propuestos

- Tasa de errores inferior al 1 %.
- Percentil 95 inferior a 2000 ms.
- Sin respuestas HTTP 500.
- Respuestas funcionales con `success=true`.

Los criterios son objetivos del experimento. Si no se cumplen, el resultado debe documentarse como una degradación o fallo de rendimiento, sin ocultar la evidencia.
