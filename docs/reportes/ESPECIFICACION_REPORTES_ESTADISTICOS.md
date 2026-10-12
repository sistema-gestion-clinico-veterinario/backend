# Reportes estadísticos clínicos y operativos

## Alcance

Esta especificación cubre las capacidades comprometidas para los reportes de SoftVet:

1. filtros;
2. comparación con un periodo anterior equivalente;
3. exportación a PDF y Excel;
4. aislamiento estricto por empresa.

El reporte clínico conserva sus filtros por periodo, profesional y especie. El reporte de ventas y productos incorpora periodo, método de pago, categoría, producto, marca y estado de stock.

## Reglas de cálculo

| Indicador operativo | Fórmula |
| --- | --- |
| Ventas | Cantidad de transacciones pagadas de tipo `TIENDA` que cumplen los filtros. Una transacción se cuenta una sola vez. |
| Ingresos sin filtro de producto | Suma de `purchase.total` de las ventas incluidas. |
| Ingresos con filtro de producto, categoría o marca | Suma de `venta_libre_detalle.subtotal` únicamente para las líneas que cumplen el filtro. No se suma el resto de una venta mixta. |
| Unidades vendidas | Suma de `venta_libre_detalle.cantidad` de las líneas incluidas. |
| Ticket promedio | Ingresos incluidos / cantidad de ventas incluidas. Si no hay ventas, el resultado es cero. |
| Stock bajo | Producto activo con `stock > 0` y `stock <= stock_minimo`. |
| Sin stock | Producto activo con `stock <= 0`. |

Los importes se redondean a dos decimales con `HALF_UP`.

## Comparación

El periodo anterior tiene exactamente la misma cantidad de días calendario y termina el día anterior al inicio del periodo actual.

Ejemplo: si el periodo actual es del 1 al 31 de julio, el periodo anterior es del 31 de mayo al 30 de junio (31 días). La variación es `(actual - anterior) / |anterior| * 100`. Si el valor anterior es cero y el actual es mayor que cero, la interfaz informa “Nuevo en este periodo” y no inventa un porcentaje.

El inventario no se compara históricamente porque SoftVet no conserva fotografías de stock por fecha. Mostrar una comparación en esas condiciones sería engañoso.

## Aislamiento multiempresa

- Para usuarios normales, el backend ignora el `companyId` enviado por el navegador y usa la empresa de la sesión autenticada.
- Solo el administrador de plataforma puede seleccionar una empresa explícitamente.
- Las compras y los productos se consultan siempre con el `companyId` resuelto en el backend.
- Un identificador de producto, categoría o marca perteneciente a otra empresa produce un conjunto vacío; nunca amplía el alcance.
- El alcance `OWN` limita las ventas a las registradas por el usuario actual y no expone el inventario general de la clínica.

## Matriz de validación

| Capacidad | Caso de control | Resultado esperado |
| --- | --- | --- |
| Filtro por periodo | Ventas dentro y fuera del rango | Solo se incluyen las fechas solicitadas. |
| Filtro por método | Venta Yape y venta Efectivo | Al elegir Yape solo se incluye la venta Yape. |
| Filtro por producto | Venta mixta de S/ 100 con una línea coincidente de S/ 30 | Una venta, S/ 30 de ingreso y las unidades de esa línea. |
| Filtro por categoría/marca | Productos de dos grupos | Solo líneas y existencias del grupo elegido. |
| Estado de stock | Normal, bajo y agotado | Cada estado devuelve únicamente productos de su condición. |
| Comparación | Periodo actual y anterior con datos conocidos | Ambos resúmenes coinciden exactamente con los valores de control. |
| Multiempresa | Solicitud con `companyId` manipulado | La consulta usa la empresa de la sesión; cero registros de otra empresa. |
| PDF | Reporte operativo visible | Incluye resumen actual/anterior, productos, métodos e inventario permitidos. |
| Excel | Reporte operativo visible | Incluye hojas Resumen, Productos vendidos, Métodos de pago e Inventario. |

## Evidencia automatizada

- `ReportesClinicosServiceImplTest.reporteOperativoFiltraLineasYComparaContraUnPeriodoDeIgualDuracion`: valida cifras exactas con datos de control.
- `ReportesClinicosServiceImplTest.reporteOperativoIgnoraUnaEmpresaManipuladaEnLaUrl`: valida el aislamiento de la empresa activa.
- `reportes-clinicos.service.spec.ts`: valida el contrato HTTP de todos los filtros operativos.
- La generación de PDF y Excel registra además una acción de auditoría específica.

La evidencia de una ejecución aceptada debe registrar: versión o commit, fecha, ambiente, datos de control, pruebas ejecutadas, resultado esperado, resultado obtenido y responsable. Una compilación correcta no reemplaza la ejecución de las pruebas.
