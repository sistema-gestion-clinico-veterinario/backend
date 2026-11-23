import json
import os
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
FASE = os.environ.get('FASE_ETIQUETA', '3e')
NIV = RAIZ / 'niveles'

niveles = sorted({int(m.group(1)) for p in NIV.glob('estado_*.json') if (m := re.search(r'_(\d+)\.json$', p.name)) and 'ctrl128' not in p.name})

filas = []
datos = []
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for pref in c.prefijos_del_nivel():
        d = c.analizar(pref)
        m = c.n.metricas(d)
        valores = dict(c.valores_multi(d, m))
        ok, fallo = c.tasa_exito_escrituras(d)
        datos.append({'nivel': nivel, 'variante': pref, **{k: v for k, v in valores.items()}, 'escrituras_ok': ok, 'escrituras_fallidas': fallo, 'datos': c.estado_datos(d)})
        filas.append(
            f"| {nivel} | {c.NOMBRES_MULTI.get(pref, pref)} | {c.n.veredicto(m).split(' (')[0]} | {valores['Errores (%)']} | {valores['Inicios de sesión con error (%)']} | {valores['Inicios de sesión: p95 (ms)']} | {valores['Ritmo medio (peticiones/s)']} | "
            f"{valores['Tiempo p95 (ms)']} | {valores['Tiempo p99 (ms)']} | {valores['Panel bajo carga: p95 (ms)']} | {valores['CPU total por 1.000 peticiones (s)']} | {valores['Tiempo en GC']} | "
            f"{valores['Planes lentos muestreados (>= 300 ms)']} / {valores['...de ellos con compilación JIT']} | {valores['Avisos de pool agotado']} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )

TITULOS = {'3e': 'Fase 3e: configuración de PostgreSQL y heap', '3f': 'Fase 3f: inicio de sesión por fases frente al anterior'}
lineas = [f"# {TITULOS.get(FASE, 'Fase ' + FASE)}", '']
lineas += c.lineas_entorno() + ['']
lineas += [
    '| Usuarios | Variante | Veredicto | Errores | Login con error | Login p95 (ms) | Peticiones/s | p95 (ms) | p99 (ms) | Panel p95 (ms) | CPU total por 1.000 peticiones (s) | Tiempo en GC | Planes lentos / con JIT | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|',
] + filas + ['']
if FASE == '3f':
    lineas.append('> Cada corrida parte de la misma base de la plantilla, con la misma configuración; solo cambia la versión del código del inicio de sesión. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo (mismo equipo).')
else:
    lineas.append('> Cada corrida parte de la misma base de la plantilla y de la misma imagen. "Plan por consulta" = plan_cache_mode=force_custom_plan; "sin JIT" = jit=off. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo.')
(RAIZ / f'RESUMEN_FASE{FASE.upper()}.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / f'RESUMEN_FASE{FASE.upper()}.json').write_text(json.dumps(datos, indent=1, ensure_ascii=False), encoding='utf-8')
print('\n'.join(lineas))
