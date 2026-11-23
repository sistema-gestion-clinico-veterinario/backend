import html
import json
import os
import sys

sal, escenario = sys.argv[1], sys.argv[2]
base = os.path.join(sal, 'visual')
TITULOS = {
    'caida': 'Caída de la base de datos vista desde el navegador',
    'login': 'Inicio de sesión durante una ráfaga de contraseñas incorrectas',
    'panel': 'Carga del panel principal',
}
VERSIONES = [('antes', 'Antes'), ('despues', 'Después')]


def cargar(v):
    ruta = os.path.join(base, f'{escenario}_{v}', 'resultado.json')
    if not os.path.exists(ruta):
        return None
    with open(ruta, encoding='utf-8') as f:
        return json.load(f)


def aplanar(valor, prefijo=''):
    if isinstance(valor, dict):
        for k, v in valor.items():
            yield from aplanar(v, f'{prefijo}{k}.' if isinstance(v, dict) else f'{prefijo}{k}')
    else:
        yield prefijo.rstrip('.'), valor


def capturas(v):
    carpeta = os.path.join(base, f'{escenario}_{v}')
    if not os.path.isdir(carpeta):
        return []
    return sorted(f for f in os.listdir(carpeta) if f.endswith('.png'))


datos = {v: cargar(v) for v, _ in VERSIONES}
md = [f'## {TITULOS.get(escenario, escenario)}', '']
for v, nombre in VERSIONES:
    d = datos[v]
    md.append(f'### {nombre}')
    md.append('')
    if d is None:
        md += ['No hay resultado para esta versión.', '']
        continue
    md += ['| Dato | Valor |', '|---|---|']
    for k, val in aplanar(d['resumen']):
        md.append(f'| {k} | {json.dumps(val, ensure_ascii=False) if isinstance(val, (list, dict)) else val} |')
    md += ['', '| Segundos | Paso | Dirección | ¿Se ve el formulario de login? | Mensajes en pantalla |', '|---|---|---|---|---|']
    for o in d['observaciones']:
        md.append(f"| {o['t']} | {o['paso']} | `{o['url']}` | {'sí' if o['formularioDeLogin'] else 'no'} | {' / '.join(o['mensajes']) or '-'} |")
    if d['errores']:
        md += ['', 'Incidencias del guion:'] + [f'- {e}' for e in d['errores']]
    md.append('')
md.append('Las capturas y el video están en el artefacto, en `visual/` (abrir `INFORME_' + escenario + '.html`).')
with open(os.path.join(base, f'INFORME_{escenario}.md'), 'w', encoding='utf-8') as f:
    f.write('\n'.join(md) + '\n')

pasos = sorted(set(capturas('antes')) | set(capturas('despues')))
filas = []
for p in pasos:
    celdas = []
    for v, nombre in VERSIONES:
        existe = p in capturas(v)
        celdas.append(
            f'<td><figure><img src="{escenario}_{v}/{html.escape(p)}" loading="lazy"><figcaption>{nombre}</figcaption></figure></td>'
            if existe else '<td class="vacio">sin captura</td>')
    filas.append(f'<tr><th>{html.escape(p[:-4])}</th>{"".join(celdas)}</tr>')

with open(os.path.join(base, f'INFORME_{escenario}.html'), 'w', encoding='utf-8') as f:
    f.write(f'''<!doctype html><html lang="es"><meta charset="utf-8"><title>{html.escape(TITULOS.get(escenario, escenario))}</title>
<style>body{{font-family:system-ui,sans-serif;margin:24px;color:#0f172a}}table{{border-collapse:collapse;width:100%}}
th,td{{border:1px solid #cbd5e1;padding:8px;vertical-align:top}}th{{width:14%;text-align:left;background:#f1f5f9}}
img{{width:100%;height:auto;border:1px solid #e2e8f0}}figcaption{{font-size:13px;color:#475569;margin-top:4px}}.vacio{{color:#64748b}}
pre{{background:#f8fafc;padding:12px;overflow:auto}}</style>
<h1>{html.escape(TITULOS.get(escenario, escenario))}</h1>
<table><tr><th>Paso</th><th>Antes</th><th>Después</th></tr>{"".join(filas)}</table>
<h2>Datos medidos</h2><pre>{html.escape(chr(10).join(md))}</pre></html>''')
print('informe visual generado')
