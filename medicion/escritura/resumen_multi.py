import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
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
            f"| {nivel} | {c.NOMBRES_MULTI.get(pref, pref)} | {c.n.veredicto(m).split(' (')[0]} | {valores['Errores (%)']} | {valores['Ritmo medio (peticiones/s)']} | "
            f"{valores['Tiempo p95 (ms)']} | {valores['Panel bajo carga: p95 (ms)']} | {valores['CPU total por 1.000 peticiones (s)']} | {valores['Tiempo en GC']} | "
            f"{valores['Planes lentos muestreados (>= 300 ms)']} / {valores['...de ellos con compilación JIT']} | {valores['Avisos de pool agotado']} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )

lineas = ['# Fase 3e: configuración de PostgreSQL y heap', '']
lineas += c.lineas_entorno() + ['']
lineas += [
    '| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Panel p95 (ms) | CPU total por 1.000 peticiones (s) | Tiempo en GC | Planes lentos / con JIT | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |',
    '|---|---|---|---|---|---|---|---|---|---|---|---|---|',
] + filas + ['']
lineas.append('> Cada corrida parte de la misma base de la plantilla y de la misma imagen. "Plan por consulta" = plan_cache_mode=force_custom_plan; "sin JIT" = jit=off. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo.')
(RAIZ / 'RESUMEN_FASE3E.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
(RAIZ / 'RESUMEN_FASE3E.json').write_text(json.dumps(datos, indent=1, ensure_ascii=False), encoding='utf-8')
print('\n'.join(lineas))
