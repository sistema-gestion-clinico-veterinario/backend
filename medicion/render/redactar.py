import os
import sys
from pathlib import Path
from urllib.parse import urlparse

raiz = Path(sys.argv[1])
sustituciones = []


def agregar(valor, marca):
    valor = (valor or '').strip()
    if len(valor) >= 4:
        sustituciones.append((valor, marca))


for nombre, marca in (('URL_ANTES', '<url-antes>'), ('URL_DESPUES', '<url-despues>')):
    url = os.environ.get(nombre, '').strip().rstrip('/')
    agregar(url, marca)
    agregar(urlparse(url).netloc, marca)

for nombre, marca in (('DB_ANTES', 'antes'), ('DB_DESPUES', 'despues')):
    cadena = os.environ.get(nombre, '').strip()
    agregar(cadena, f'<base-{marca}>')
    destino = urlparse(cadena)
    agregar(destino.password, '<clave>')
    agregar(destino.hostname, f'<servidor-{marca}>')
    agregar(destino.username, f'<usuario-{marca}>')
    if destino.username and '.' in destino.username:
        agregar(destino.username.split('.', 1)[1], f'<proyecto-{marca}>')

agregar(os.environ.get('MEDICION_CLAVE', ''), '<clave>')
sustituciones.sort(key=lambda par: len(par[0]), reverse=True)

for ruta in raiz.rglob('*'):
    if not ruta.is_file():
        continue
    try:
        texto = ruta.read_text(encoding='utf-8', errors='replace')
    except OSError:
        continue
    nuevo = texto
    for valor, marca in sustituciones:
        nuevo = nuevo.replace(valor, marca)
    if nuevo != texto:
        ruta.write_text(nuevo, encoding='utf-8')
