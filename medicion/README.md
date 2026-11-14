# Medición A/B del servidor

Compara el backend como está en `main` (**antes**) con `main` más los cambios de green software, rama `medicion/despues` (**después**).
Se ejecuta en GitHub Actions: no necesita Render, Supabase ni ningún equipo local.

## Cómo se lanza
Subir esta rama dispara el flujo `Medición A/B del servidor`:

```bash
git push -u origin medicion/ab
```

También se puede lanzar a mano desde la pestaña Actions si el flujo está en la rama por defecto.

## Qué hace
1. Construye las dos imágenes con `Dockerfile.measurement` (mismas banderas de JVM).
2. Levanta un PostgreSQL 16 con dos bases (`antes`, `despues`), crea el esquema con el propio backend, siembra los mismos datos (`sembrar.sql`) y crea la cuenta de medición (`provisionar_cuenta.sql`). Cada base recibe además las migraciones de su propia versión.
3. Alterna las corridas (antes, después, antes, …) con un calentamiento descartado y 5 rondas. El orden se invierte en las rondas pares.
4. Por corrida registra segundos de CPU, memoria máxima, tiempo de arranque, sentencias SQL (`pg_stat_statements`) y latencia por recorrido (k6).
5. Escribe `resultados/RESUMEN.md` y lo publica en la rama `medicion/resultados`.

## Qué mide y qué no
- Mide el backend y su base en un Linux de GitHub, con CPU fijada por contenedor. Sirve para comparar versiones, no como cifra absoluta de producción.
- No mide energía ni emisiones, ni Render, Supabase o el servicio de IA.
- Los datos no incluyen productos ni detalle de ventas: el reporte de ventas se mide con los pagos generados.
