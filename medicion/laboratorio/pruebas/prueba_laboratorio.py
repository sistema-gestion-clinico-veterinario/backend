import gzip
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import comparar_jstat as cj  # noqa: E402
import hsperfdata as hs  # noqa: E402
import informe_laboratorio as inf  # noqa: E402
import muestreo_generador as mg  # noqa: E402
import muestreo_jvm as mj  # noqa: E402
import topologia as tp  # noqa: E402

FIXTURE = Path(__file__).resolve().parent / 'hsperfdata_jdk22_serial.bin.gz'


def contenedor(perf, actual=1073741824, anon=805306368, archivo=104857600, pico=1181116006):
    texto = "\n".join(["--actual", str(actual), "--stat", f"anon {anon}", f"file {archivo}", "--pico", str(pico)]) + "\n"
    return perf + mj.MARCA + texto.encode()


class MuestreoJvm(unittest.TestCase):
    def test_contadores_de_una_jvm_real_coinciden_con_jstat(self):
        perf = gzip.decompress(FIXTURE.read_bytes())
        c = hs.leer_contadores(perf)
        self.assertEqual(c['sun.gc.generation.1.space.0.name'], 'old')
        r = mj.parsear_muestra(contenedor(perf))
        self.assertEqual(r['fuente'], 'hsperfdata')
        self.assertEqual(r['heap_comprometido_kb'], 2560 + 2560 + 20864 + 51868)
        self.assertAlmostEqual(r['viejo_usado_kb'], 43921.5, delta=1)
        self.assertEqual(r['metaspace_comprometido_kb'], 9792)
        self.assertEqual(r['ygc'], 7)
        self.assertEqual(r['fgc'], 2)
        self.assertAlmostEqual(r['gct_s'], 0.1178, places=3)

    def test_separa_heap_contenedor_y_gc(self):
        perf = gzip.decompress(FIXTURE.read_bytes())
        r = mj.parsear_muestra(contenedor(perf))
        self.assertEqual(r['cont_actual_kb'], 1048576)
        self.assertEqual(r['cont_anon_kb'], 786432)
        self.assertEqual(r['cont_file_kb'], 102400)
        self.assertEqual(r['cont_pico_kb'], round(1181116006 / 1024))
        self.assertLess(r['heap_usado_kb'], r['heap_comprometido_kb'])

    def test_comparador_con_jstat_real(self):
        perf = gzip.decompress(FIXTURE.read_bytes())
        fila = mj.parsear_muestra(contenedor(perf))
        j = cj.leer_jstat((Path(__file__).resolve().parent / 'jstat_jdk22.txt').read_text(encoding='utf-8'))
        resultado = {nombre: ok for nombre, _, _, ok in cj.comparar(j, fila)}
        self.assertTrue(resultado['Heap comprometido (KB)'])
        self.assertTrue(resultado['Metaspace comprometido (KB)'])
        self.assertTrue(resultado['Recolecciones young'])
        self.assertTrue(resultado['Recolecciones full'])

    def test_comparador_detecta_diferencias(self):
        j = {'S0C': 100.0, 'S1C': 100.0, 'EC': 800.0, 'OC': 2000.0, 'S0U': 0.0, 'S1U': 50.0, 'EU': 400.0, 'OU': 1000.0, 'MU': 5000.0, 'MC': 5200.0, 'YGC': 3.0, 'FGC': 1.0, 'GCT': 0.2}
        fila = {'heap_comprometido_kb': 3000, 'heap_usado_kb': 1450, 'metaspace_usado_kb': 5000, 'metaspace_comprometido_kb': 5200, 'ygc': 3, 'fgc': 1, 'gct_s': 0.2}
        self.assertTrue(all(ok for *_, ok in cj.comparar(j, fila)))
        fila['heap_usado_kb'] = 1600
        self.assertFalse(dict((n, ok) for n, _, _, ok in cj.comparar(j, fila))['Heap usado (KB)'])

    def test_sin_contadores_conserva_la_memoria_del_contenedor(self):
        r = mj.parsear_muestra(contenedor(b'', actual=2097152, anon=1048576, archivo=0, pico=0))
        self.assertEqual(r['fuente'], 'ninguna')
        self.assertEqual(r['heap_usado_kb'], '')
        self.assertEqual(r['cont_actual_kb'], 2048)
        self.assertEqual(r['cont_pico_kb'], 0)

    def test_archivo_corrupto_no_rompe(self):
        self.assertIsNone(hs.leer_contadores(b'\x00' * 100))
        self.assertIsNone(hs.leer_contadores(b''))
        self.assertEqual(mj.parsear_muestra(b'basura sin marca')['fuente'], 'ninguna')


class MuestreoGenerador(unittest.TestCase):
    def crear_proc(self, raiz, pid, utime, stime, rss, hwm, hilos, cpus_cadena, comm='k6'):
        d = raiz / str(pid)
        d.mkdir(parents=True)
        (d / 'comm').write_text(comm + '\n')
        campos = ['S', '1', '1', '1', '0', '-1', '0', '0', '0', '0', '0', str(utime), str(stime), '0', '0', '20', '0', str(hilos)]
        (d / 'stat').write_text(f'{pid} ({comm}) ' + ' '.join(campos) + ' 0 0\n')
        (d / 'status').write_text(f'Name:\t{comm}\nVmHWM:\t{hwm} kB\nVmRSS:\t{rss} kB\nCpus_allowed_list:\t{cpus_cadena}\nMems_allowed_list:\t0\n')

    def test_lee_ticks_rss_y_afinidad(self):
        with tempfile.TemporaryDirectory() as t:
            raiz = Path(t)
            self.crear_proc(raiz, 321, 700, 300, 150000, 180000, 14, '0')
            self.crear_proc(raiz, 5, 1, 1, 1, 1, 1, '0-3', comm='otro')
            self.assertEqual(mg.buscar_pid(raiz, 'k6'), 321)
            self.assertIsNone(mg.buscar_pid(raiz, 'inexistente'))
            self.assertEqual(mg.ticks_proceso(raiz, 321), (1000, 14))
            e = mg.estado_proceso(raiz, 321)
            self.assertEqual(mg.kb(e['VmRSS']), 150000)
            self.assertEqual(e['Cpus_allowed_list'], '0')

    def test_cpu_sistema_por_cpu_logica(self):
        with tempfile.TemporaryDirectory() as t:
            raiz = Path(t)
            (raiz / 'stat').write_text('cpu 100 0 100 700 100 0 0 0 0 0\ncpu0 50 0 50 300 0 0 0 0 0 0\ncpu1 50 0 50 400 100 0 0 0 0 0\nintr 1\n')
            c = mg.cpus_sistema(raiz)
            self.assertEqual(c['cpu0'], (400, 100))
            self.assertEqual(c['cpu1'], (600, 100))
            self.assertAlmostEqual(mg.porcentaje((500, 200), (400, 100)), 100.0)
            self.assertIsNone(mg.porcentaje((400, 100), (400, 100)))


class Topologia(unittest.TestCase):
    def raiz(self, t, hermanos):
        for n, h in hermanos.items():
            d = Path(t) / f'cpu{n}' / 'topology'
            d.mkdir(parents=True)
            if h is not None:
                (d / 'thread_siblings_list').write_text(h + '\n')
            (d / 'core_id').write_text(str(n % 2) + '\n')
            (d / 'physical_package_id').write_text('0\n')

    def test_detecta_nucleos_compartidos(self):
        with tempfile.TemporaryDirectory() as t:
            self.raiz(t, {0: '0,2', 1: '1,3', 2: '0,2', 3: '1,3'})
            cpus = tp.leer_topologia(t)
            a = tp.analizar(cpus, {'k6': [0], 'backend': [1], 'postgres': [2]})
            self.assertTrue(a['smt'])
            pares = {(c['a'], c['b']) for c in a['roles_que_comparten_nucleo_fisico']}
            self.assertEqual(pares, {('k6', 'postgres')})
            md = tp.a_markdown(a, cpus)
            self.assertIn('k6 y postgres comparten núcleo físico', md)
            self.assertIn('Aislamiento físico no demostrado', md)

    def test_sin_conflicto_y_sin_datos(self):
        with tempfile.TemporaryDirectory() as t:
            self.raiz(t, {0: '0-1', 1: '0-1', 2: '2-3', 3: '2-3'})
            a = tp.analizar(tp.leer_topologia(t), {'k6': [0], 'postgres': [2]})
            self.assertEqual(a['roles_que_comparten_nucleo_fisico'], [])
        with tempfile.TemporaryDirectory() as t:
            self.raiz(t, {0: None, 1: None})
            cpus = tp.leer_topologia(t)
            a = tp.analizar(cpus, {'k6': [0]})
            self.assertTrue(a['topologia_desconocida'])
            self.assertIn('No se pudo leer', tp.a_markdown(a, cpus))

    def test_listas_de_cpu(self):
        self.assertEqual(tp.lista_cpus('0,3'), [0, 3])
        self.assertEqual(tp.lista_cpus('0-2,5'), [0, 1, 2, 5])
        self.assertEqual(tp.lista_cpus(''), [])


class SaludGenerador(unittest.TestCase):
    def preparar(self, t, etiqueta, nivel, cpu, vus_activos, codigo=0, descartadas=None, afinidad='0', sin_resumen=False):
        niv = Path(t) / 'niveles'
        niv.mkdir(exist_ok=True)
        (niv / f'estado_{etiqueta}.json').write_text(json.dumps({'nivel': nivel, 'k6_codigo_salida': codigo}))
        if not sin_resumen:
            (niv / f'k6_{etiqueta}.json').write_text(json.dumps({
                'peticiones': 1000, 'iteraciones_completas': 400, 'duracion_total_s': 100, 'vus_maximos': nivel, 'vus_activos_max': vus_activos,
                'iteraciones_descartadas': descartadas, 'estados': {'2xx': 990, '5xx': 5, 'sin_respuesta': 5}}))
        if cpu is not None:
            filas = ['ts,cpu_k6_pct,rss_kb,hwm_kb,hilos,cpu_sistema_pct,cpus_logicas'] + [f'{i},{c},200000,210000,10,20,0:{c}' for i, c in enumerate(cpu)]
            (niv / f'generador_{etiqueta}.csv').write_text('\n'.join(filas) + '\n')
            (niv / f'generador_{etiqueta}.csv.afinidad').write_text(f'pid=1\nCpus_allowed_list={afinidad}\nhz=100\n')
        return niv

    def test_generador_sano(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'despues_250', 250, [30, 40, 35, 50], 250)
            r = inf.evaluar_generador(niv, 'despues_250')
            self.assertFalse(r['saturado'])
            self.assertEqual(r['interpretable'], 'sí')
            self.assertEqual(r['rss_pico_mb'], 210000 / 1024)
            self.assertAlmostEqual(r['peticiones_por_s'], 10.0)
            self.assertAlmostEqual(r['no_2xx_pct'], 1.0)

    def test_saturacion_requiere_ventana_consecutiva(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 100, [95, 10, 95, 10, 95, 10], 100)
            self.assertFalse(inf.evaluar_generador(niv, 'a')['saturado'])
            niv = self.preparar(t, 'b', 100, [10, 85, 90, 88, 10], 100)
            r = inf.evaluar_generador(niv, 'b')
            self.assertTrue(r['saturado'])
            self.assertEqual(r['interpretable'], 'potencialmente no')
            self.assertIn('por encima de 80 %', r['motivos'][0])

    def test_umbral_configurable(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 100, [60, 65, 70, 62], 100)
            self.assertFalse(inf.evaluar_generador(niv, 'a', 80)['saturado'])
            self.assertTrue(inf.evaluar_generador(niv, 'a', 50)['saturado'])

    def test_porcentaje_se_mide_sobre_los_nucleos_fijados(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 100, [120, 130, 110], 100, afinidad='0,3')
            r = inf.evaluar_generador(niv, 'a')
            self.assertEqual(r['nucleos_k6'], 2)
            self.assertFalse(r['saturado'])
            niv = self.preparar(t, 'b', 100, [120, 130, 110], 100, afinidad='0')
            self.assertTrue(inf.evaluar_generador(niv, 'b')['saturado'])

    def test_no_alcanza_la_carga_prevista(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 250, [20, 20, 20], 180)
            r = inf.evaluar_generador(niv, 'a')
            self.assertFalse(r['alcanzo_vus'])
            self.assertEqual(r['interpretable'], 'potencialmente no')

    def test_codigo_de_k6_y_descartadas(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 100, [20, 20, 20], 100, codigo=99)
            self.assertEqual(inf.evaluar_generador(niv, 'a')['interpretable'], 'potencialmente no')
            niv = self.preparar(t, 'b', 100, [20, 20, 20], 100, descartadas=7)
            self.assertEqual(inf.evaluar_generador(niv, 'b')['interpretable'], 'potencialmente no')

    def test_sin_muestras_es_no_evaluado(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'a', 100, None, 100)
            r = inf.evaluar_generador(niv, 'a')
            self.assertIsNone(r['saturado'])
            self.assertEqual(r['interpretable'], 'no evaluado')
            niv = self.preparar(t, 'b', 100, [10, 10, 10], 100, sin_resumen=True)
            self.assertEqual(inf.evaluar_generador(niv, 'b')['interpretable'], 'no evaluado')

    def test_informe_completo(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'despues_50', 50, [30, 40, 35], 50)
            (niv / 'jvm_vivo_despues_50.csv').write_text(
                ','.join(mj.ORDEN) + '\n' + '1,120,hsperfdata,100000,300000,50000,50000,60000,64000,10,0,0.2,900000,700000,150000,1000000\n'
                '6,130,hsperfdata,150000,300000,70000,80000,61000,64000,15,1,0.3,950000,720000,150000,1100000\n')
            with gzip.open(niv / 'jvm_gc_despues_50.log.gz', 'wt', encoding='utf-8') as f:
                f.write('[12.345s][info][gc] GC(3) Pause Young (Allocation Failure) 85M->12M(300M) 12.345ms\n')
                f.write('[20.000s][info][gc] GC(4) Pause Full (Allocation Failure) 290M->100M(300M) 150.500ms\n')
            texto = inf.generar(t, 80)
            self.assertIn('Salud del laboratorio', texto)
            self.assertIn('despues_50', texto)
            self.assertIn('| sí |'.replace('| sí |', '**sí**'), texto)
            j = json.loads((niv / 'jvm_resumen_despues_50.json').read_text(encoding='utf-8'))
            self.assertAlmostEqual(j['heap_usado_max_mb'], 150000 / 1024)
            self.assertAlmostEqual(j['heap_comprometido_max_mb'], 300000 / 1024)
            self.assertEqual(j['jstat_ygc'], 5)
            self.assertEqual(j['jstat_fgc'], 1)
            self.assertEqual(j['gc_registro']['pausas'], 2)
            self.assertEqual(j['gc_registro']['completas'], 1)
            self.assertAlmostEqual(j['gc_registro']['max_ms'], 150.5)
            self.assertTrue((Path(t) / 'SALUD_LABORATORIO.md').exists())

    def test_informe_sin_herramientas_java(self):
        with tempfile.TemporaryDirectory() as t:
            niv = self.preparar(t, 'despues_50', 50, [30, 40, 35], 50)
            (niv / 'jvm_vivo_despues_50.csv').write_text(
                ','.join(mj.ORDEN) + '\n' + '1,120,ninguna,,,,,,,,,,900000,700000,150000,1000000\n')
            inf.generar(t, 80)
            j = json.loads((niv / 'jvm_resumen_despues_50.json').read_text(encoding='utf-8'))
            self.assertFalse(j['heap_en_vivo'])
            self.assertEqual(j['sin_jstat'], 1)
            self.assertIn('no medido', (Path(t) / 'SALUD_LABORATORIO.md').read_text(encoding='utf-8'))


if __name__ == '__main__':
    unittest.main()
