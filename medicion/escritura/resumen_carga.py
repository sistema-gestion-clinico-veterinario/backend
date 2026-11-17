import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import informe_carga as c  # noqa: E402

RAIZ = Path(sys.argv[1])
NIV = RAIZ / 'niveles'

niveles = sorted({int(p.name.split('_')[-1].split('.')[0]) for p in NIV.glob('estado_antes_*.json')} |
                 {int(p.name.split('_')[-1].split('.')[0]) for p in NIV.glob('estado_despues_*.json')})

lineas = ['# Fase 2: resumen de la escalera de carga con escrituras', '']
lineas.append('| Usuarios | Versión | Veredicto | Último tramo sano | Errores | p95 (ms) | Escrituras correctas / fallidas | Datos |')
lineas.append('|---|---|---|---|---|---|---|---|')
for nivel in niveles:
    c.configurar(RAIZ, nivel)
    for version, titulo in (('antes', 'ANTES'), ('despues', 'DESPUÉS')):
        d = c.analizar(version)
        m = c.n.metricas(d)
        ok, fallo = c.tasa_exito_escrituras(d)
        sano = f"~{m['sano']:.0f} usuarios" if m['sano'] is not None else '-'
        lineas.append(
            f"| {nivel} | {titulo} | {c.n.veredicto(m).split(' (')[0]} | {sano} | {c.n.fmt(m['errores_pct'], 1, ' %')} | "
            f"{c.n.fmt(m['p95_ms'])} | {ok} / {fallo} | {c.estado_datos(d)} |"
        )
lineas.append('')
lineas.append('> Datos CONSISTENTES = lo que k6 contó como creado coincide con la base y no hay registros huérfanos. Cada nivel reinicia el backend; la base conserva lo escrito en niveles anteriores.')
(RAIZ / 'RESUMEN_FASE2.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
print('\n'.join(lineas))
