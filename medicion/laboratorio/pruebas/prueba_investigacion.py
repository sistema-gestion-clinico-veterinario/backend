import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
sys.path.insert(0, str(Path(__file__).resolve().parent))

import comparar_investigacion as ci  # noqa: E402
from prueba_comparacion import construir  # noqa: E402

NMT = """Native Memory Tracking:

(Omitting categories weighting less than 1KB)

Total: reserved=900000KB, committed=420000KB

-                 Java Heap (reserved=262144KB, committed=262144KB)
-                     Class (reserved=1048576KB, committed=110000KB)
-                    Thread (reserved=40000KB, committed=15000KB)
-                      Code (reserved=49152KB, committed=30000KB)
-                        GC (reserved=12000KB, committed=9000KB)
"""


def inv(raiz, tipo, rep, etiqueta, nivel, pool=10, pg=False, nmt=False, **k):
    d = construir(raiz, 'PRODUCCION-512M', rep, nivel, **k)
    destino = Path(raiz) / f'inv-{tipo}-r{rep}-{etiqueta}'
    d.rename(destino)
    with open(destino / 'entorno.txt', 'a', encoding='utf-8') as f:
        f.write(f'pool de conexiones: {pool}\n')
    if pg:
        (destino / 'niveles' / f'pg_actividad_despues_{nivel}.csv').write_text(
            'ts,duracion_ms,total,activas,inactivas,inactivas_en_transaccion,esperan_lock,esperan_io,esperan_cliente,tx_mas_vieja_s,xact_commit,xact_rollback\n'
            '1000,20,10,10,0,0,0,0,0,1.5,100,1\n1002,20,10,6,2,2,1,0,0,30.5,150,1\n', encoding='utf-8')
    if nmt:
        (destino / 'niveles' / f'nmt_despues_{nivel}.txt').write_text(NMT, encoding='utf-8')
    return destino


class Investigacion(unittest.TestCase):
    def test_descubre_solo_directorios_inv(self):
        with tempfile.TemporaryDirectory() as t:
            inv(t, 'heap', 2, 'EXP-HEAP-256M', 50)
            construir(t, 'CAPACIDAD-2G', 1, 100)
            r = ci.descubrir(t)
            self.assertEqual([(x[1], x[2], x[3], x[4]) for x in r], [('heap', 2, 'EXP-HEAP-256M', 50)])

    def test_lee_nmt_actividad_pg_y_pool(self):
        with tempfile.TemporaryDirectory() as t:
            d = inv(t, 'pool', 1, 'POOL-40', 250, pool=40, pg=True, nmt=True, heap_max='1g', limite_mb=2048)
            c = ci.analizar(d, 'pool', 1, 'POOL-40', 250)
            self.assertEqual(c['pool'], 40)
            self.assertEqual(c['pg_actividad']['activas_max'], 10)
            self.assertEqual(c['pg_actividad']['inactivas_en_transaccion_max'], 2)
            self.assertAlmostEqual(c['pg_actividad']['tx_mas_vieja_max_s'], 30.5)
            self.assertAlmostEqual(c['nmt']['total_comprometido_mb'], 420000 / 1024)
            self.assertAlmostEqual(c['nmt']['categorias_mb']['Thread'], 15000 / 1024)

    def test_informe_completo_de_los_tres_tipos(self):
        with tempfile.TemporaryDirectory() as t:
            for rep in (1, 2, 3):
                for e in ('EXP-HEAP-192M', 'EXP-HEAP-256M'):
                    inv(t, 'heap', rep, e, 50, nmt=(rep == 1))
                for p in (10, 40):
                    inv(t, 'pool', rep, f'POOL-{p}', 250, pool=p, pg=True, heap_max='1g', limite_mb=2048)
                for modo in ('topologia', 'fijo'):
                    for perfil in ('CAPACIDAD-2G', 'PRODUCCION-512M'):
                        inv(t, 'afin', rep, f'{modo}-{perfil}', 100)
            salida = Path(t) / 'salida'
            texto = ci.generar(t, salida)
            for titulo in ('## 2. Investigación del heap', '## 3. Investigación del pool', '## 4. Comparación de afinidad', '### Diferencia por repetición', '### Memoria nativa según NMT', '### Conexiones (PostgreSQL y Hikari)'):
                self.assertIn(titulo, texto)
            self.assertTrue((salida / 'comparacion_investigacion.csv').exists())
            self.assertEqual(len((salida / 'comparacion_investigacion.csv').read_text(encoding='utf-8').strip().splitlines()), 1 + 3 * (2 + 2 + 4))

    def test_sin_corridas(self):
        with tempfile.TemporaryDirectory() as t:
            self.assertIn('Sin corridas de este tipo', ci.generar(t, t))


if __name__ == '__main__':
    unittest.main()
