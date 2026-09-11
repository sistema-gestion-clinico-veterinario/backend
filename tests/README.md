# Organización de pruebas ERS

Este directorio documenta la organización transversal solicitada por el plan de QA. Para respetar las convenciones del proyecto, el código automatizado no se duplicará aquí:

- backend Java: `src/test/java/veterinaria/vargasvet/ers/<modulo>/`;
- frontend Angular unitario: archivos `*.spec.ts` junto al código probado;
- frontend E2E: `e2e/ers/<modulo>/` en el repositorio frontend;
- rendimiento: herramientas del Instrumento 1 y pruebas bajo `ers/nonfunctional/`;
- documentación y evidencias: `docs/testing/`.

Los módulos previstos son autenticación, RBAC, personal, clientes, mascotas, citas, historias clínicas, preventivos, pagos y no funcionales. La creación de cada suite corresponde a la Fase 3 y deberá conservar el código CP en el nombre o `@DisplayName` de la prueba.

No se ejecutarán pruebas mutables contra producción por defecto.
