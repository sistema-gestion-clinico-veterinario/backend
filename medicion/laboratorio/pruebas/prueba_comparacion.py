import gzip
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import comparar_bateria as cb  # noqa: E402


def construir(raiz, perfil, rep, nivel, *, errores_2xx=None, oom=False, reinicios=0, salud='200', caida='', informes=3, p_informe=2500,
              heap_max='192m', limite_mb=512, vus=None, gen_cpu=10, oom_log=False, io=True, k6_codigo=0):
    d = Path(raiz) / f'bateria-r{rep}-{perfil}'
    niv = d / 'niveles'
    (d / 'logs').mkdir(parents=True, exist_ok=True)
    niv.mkdir(exist_ok=True)
    (d / 'entorno.txt').write_text(f'procesador: Prueba CPU; hilos por núcleo: 2\nperfil del laboratorio: {perfil}\nmodo de afinidad: topologia (aplicado: si)\nmezcla de operaciones: semilla 7\n', encoding='utf-8')
    et = f'despues_{nivel}'
    peticiones = 1000
    ok2 = peticiones if errores_2xx is None else errores_2xx
    (niv / f'estado_{et}.json').write_text(json.dumps({
        'nivel': nivel, 'perfil_lab': perfil, 'cpus_limite': 1, 'memoria_limite_mb': limite_mb, 'heap_max': heap_max, 'oom_killed': oom, 'reinicios': reinicios,
        'sigue_corriendo': not caida, 'codigo_salida': 137 if oom else 0, 'salud_final_http': salud, 'k6_codigo_salida': k6_codigo, 'duracion_s': 100,
        'caida_backend': caida, 'sql_sentencias': 5000, 'sql_tiempo_s': 3.5}), encoding='utf-8')
    (niv / f'k6_{et}.json').write_text(json.dumps({
        'peticiones': peticiones, 'iteraciones_completas': 300, 'duracion_total_s': 100, 'vus_maximos': nivel, 'vus_activos_max': nivel if vus is None else vus,
        'iteraciones_descartadas': None, 'latencia_ms': {'mediana': 30, 'p95': 900, 'p99': 1500, 'max': 4000}, 'estados': {'2xx': ok2, '5xx': peticiones - ok2, 'sin_respuesta': 0},
        'acciones': {'panel': 10, 'reportes': informes}}), encoding='utf-8')
    filas = ['metric_name,timestamp,metric_value,check,error,error_code,expected_response,group,method,name,proto,scenario,service,status,subproto,tls_version,url,extra_tags,metadata']
    for i in range(informes):
        filas.append(f'http_req_duration,{1000 + 20 * i + 3},{p_informe + i * 100},,,,,,GET,,,,,200,,,,ruta=reporte_clinico,')
    for i in range(30):
        filas.append(f'http_req_duration,{1000 + 2 * i},{40 + i},,,,,,GET,,,,,200,,,,ruta=panel,')
    with gzip.open(niv / f'k6_{et}.csv.gz', 'wt', encoding='utf-8') as f:
        f.write('\n'.join(filas) + '\n')
    (niv / f'stats_{et}.csv').write_text('1000,50.00%,300MiB / 512MiB\n1005,96.00%,400MiB / 512MiB\n', encoding='utf-8')
    (niv / f'stats_pg_{et}.csv').write_text('1000,10.00%,300MiB / 2GiB\n', encoding='utf-8')
    (niv / f'jvm_vivo_{et}.csv').write_text(
        'ts,duracion_ms,fuente,heap_usado_kb,heap_comprometido_kb,eden_usado_kb,viejo_usado_kb,metaspace_usado_kb,metaspace_comprometido_kb,ygc,fgc,gct_s,cont_actual_kb,cont_anon_kb,cont_file_kb,cont_pico_kb\n'
        '1000,50,hsperfdata,100000,190000,1,1,100000,110000,1,1,0.1,400000,390000,1000,410000\n'
        '1005,50,hsperfdata,180000,190000,1,1,100000,110000,12,2,0.2,420000,410000,1000,430000\n'
        '1050,50,hsperfdata,110000,190000,1,1,100000,110000,14,2,0.3,420000,410000,1000,430000\n', encoding='utf-8')
    with gzip.open(niv / f'jvm_gc_{et}.log.gz', 'wt', encoding='utf-8') as f:
        f.write('[5.000s][info][gc] GC(1) Pause Full (Allocation Failure) 190M->150M(192M) 400.000ms\n')
        f.write('[50.000s][info][gc] GC(2) Pause Young (Allocation Failure) 120M->80M(192M) 20.000ms\n')
        f.write('[90.000s][info][gc] GC(3) Pause Full (Allocation Failure) 185M->170M(192M) 300.000ms\n')
        f.write('[100.000s][info][gc] GC(4) Pause Young (Allocation Failure) 120M->90M(192M) 10.000ms\n')
    (niv / f'generador_{et}.csv').write_text('ts,cpu_k6_pct,rss_kb,hwm_kb,hilos,cpu_sistema_pct,cpus_logicas\n' + '\n'.join(f'{i},{gen_cpu},50000,50000,10,10,0:{gen_cpu}' for i in range(10)) + '\n', encoding='utf-8')
    (niv / f'generador_{et}.csv.afinidad').write_text('pid=1\nCpus_allowed_list=0\n', encoding='utf-8')
    if io:
        def bloque(cpu, oomk, wb, rb, rx, tx):
            return (f'[backend]\nio.stat: 8:0 rbytes={rb} wbytes={wb} rios=1 wios=2 dbytes=0;\ncpu.stat: usage_usec 1;nr_periods {cpu[0]};nr_throttled {cpu[1]};throttled_usec 5;\n'
                    f'memory.events: low 0;high 0;max 0;oom 0;oom_kill {oomk};\nred: rx_bytes={rx};tx_bytes={tx}\n[postgres]\nio.stat: 8:0 rbytes={rb * 2} wbytes={wb * 3} rios=5 wios=9;\n'
                    f'cpu.stat: usage_usec 1;nr_periods 0;nr_throttled 0;\nmemory.events: oom 0;oom_kill 0;\nred: rx_bytes={rx};tx_bytes={tx}\n')
        (niv / f'io_{et}_antes.txt').write_text(bloque((100, 10), 0, 1048576, 0, 1000, 2000), encoding='utf-8')
        (niv / f'io_{et}_despues.txt').write_text(bloque((300, 110), 1 if oom else 0, 11534336, 5242880, 1049576, 5242880), encoding='utf-8')
    (d / 'logs' / f'backend_carga_{et}.log').write_text('java.lang.OutOfMemoryError: Java heap space\n' if oom_log else 'arranque normal\n', encoding='utf-8')
    return d


class Comparacion(unittest.TestCase):
    def test_lectura_de_una_corrida(self):
        with tempfile.TemporaryDirectory() as t:
            construir(t, 'PRODUCCION-512M', 1, 250)
            [(d, perfil, rep, nivel)] = cb.descubrir(t)
            c = cb.analizar_corrida(d, perfil, rep, nivel)
            self.assertEqual((perfil, rep, nivel), ('PRODUCCION-512M', 1, 250))
            self.assertAlmostEqual(c['rps'], 10.0)
            self.assertEqual(c['p99_ms'], 1500)
            self.assertEqual(c['informe_clinico']['n'], 3)
            self.assertEqual(c['informe_clinico']['p95'], 2700)
            self.assertAlmostEqual(c['cpu_backend_media'], 73.0)
            self.assertAlmostEqual(c['cpu_backend_tope_pct'], 50.0)
            self.assertAlmostEqual(c['throttled_pct'], 50.0)
            self.assertAlmostEqual(c['io']['postgres']['wbytes'] / 1048576, 30.0)
            self.assertAlmostEqual(c['io']['backend']['rbytes'] / 1048576, 5.0)
            self.assertEqual(c['io']['backend']['oom_kill'], 0)
            self.assertAlmostEqual(c['heap_usado_max_mb'], 180000 / 1024)
            self.assertTrue(c['estable'])
            self.assertEqual(c['generador_interpretable'], 'sí')
            self.assertIn('topologia', c['modo_afinidad'])

    def test_gc_se_limita_a_la_ventana_del_nivel(self):
        with tempfile.TemporaryDirectory() as t:
            d = construir(t, 'PRODUCCION-512M', 1, 100)
            g = cb.pausas_en_ventana(d / 'niveles' / 'jvm_gc_despues_100.log.gz', 60, cb.lab.leer_csv(d / 'niveles' / 'jvm_vivo_despues_100.csv'))
            self.assertEqual((g['pausas'], g['completas']), (3, 1))
            self.assertAlmostEqual(g['despues_max_mb'], 170.0)
            self.assertAlmostEqual(g['max_ms'], 300.0)
            self.assertAlmostEqual(g['por_minuto'], 3.0)
            self.assertIn('posteriores al primer muestreo', g['criterio'])
            sin = cb.pausas_en_ventana(d / 'niveles' / 'jvm_gc_despues_100.log.gz', 60, None)
            self.assertIn('aproximación por tiempo', sin['criterio'])

    def test_heap_en_ventanas_de_informe(self):
        with tempfile.TemporaryDirectory() as t:
            d = construir(t, 'PRODUCCION-512M', 1, 100)
            h = cb.heap_en_informes(cb.leer_peticiones(d / 'niveles' / 'k6_despues_100.csv.gz'), cb.lab.leer_csv(d / 'niveles' / 'jvm_vivo_despues_100.csv'), 5.0)
            self.assertEqual(h['informes'], 3)
            self.assertAlmostEqual(h['heap_en_informes_max_mb'], 180000 / 1024)
            self.assertAlmostEqual(h['heap_fuera_mediana_mb'], 110000 / 1024)

    def test_oom_caida_y_registro_se_detectan(self):
        with tempfile.TemporaryDirectory() as t:
            construir(t, 'PRODUCCION-512M', 1, 250, oom=True, reinicios=1, salud='000', caida='t=2026 OOMKilled=true codigo_salida=137', oom_log=True)
            [(d, p, r, n)] = cb.descubrir(t)
            c = cb.analizar_corrida(d, p, r, n)
            self.assertFalse(c['estable'])
            self.assertEqual(c['oom_java_en_log'], 1)
            self.assertEqual(c['io']['backend']['oom_kill'], 1)
            texto = cb.conclusion(p, n, [c])
            self.assertIn('no queda demostrado', texto)
            self.assertIn('OOMKilled', texto)
            self.assertIn('OutOfMemoryError', texto)
            self.assertIn('se detuvo durante la carga', texto)

    def test_conclusion_solo_afirma_con_tres_repeticiones_limpias(self):
        with tempfile.TemporaryDirectory() as t:
            for rep in (1, 2, 3):
                construir(t, 'CAPACIDAD-2G', rep, 100, errores_2xx=1000, heap_max='1g', limite_mb=2048)
            cs = [cb.analizar_corrida(*x) for x in cb.descubrir(t)]
            for c in cs:
                c['veredicto'] = 'AGUANTA'
                c['p95_ms'] = 900
            texto = cb.conclusion('CAPACIDAD-2G', 100, cs)
            self.assertIn('cumplió los criterios experimentales', texto)
            self.assertIn('no equivale a una garantía', texto)
            self.assertIn('no queda demostrado', cb.conclusion('CAPACIDAD-2G', 100, cs[:2]))

    def test_conclusion_niega_con_errores_generador_saturado_o_heap_al_limite(self):
        with tempfile.TemporaryDirectory() as t:
            construir(t, 'PRODUCCION-512M', 1, 250, errores_2xx=900, vus=180, gen_cpu=95, heap_max='180m')
            [(d, p, r, n)] = cb.descubrir(t)
            c = cb.analizar_corrida(d, p, r, n)
            texto = cb.conclusion(p, n, [c])
            self.assertIn('no queda demostrado', texto)
            self.assertIn('180 de 250 usuarios', texto)
            self.assertIn('generador k6', texto)
            self.assertIn('el heap tras GC llegó a', texto)

    def test_informe_completo_y_archivos(self):
        with tempfile.TemporaryDirectory() as t:
            for perfil in ('CAPACIDAD-2G', 'PRODUCCION-512M'):
                for rep in (1, 2, 3):
                    for nivel in (100, 250):
                        construir(t, perfil, rep, nivel, heap_max='1g' if perfil == 'CAPACIDAD-2G' else '192m', limite_mb=2048 if perfil == 'CAPACIDAD-2G' else 512)
            salida = Path(t) / 'salida'
            salida.mkdir()
            texto = cb.generar(t, salida)
            for titulo in ('## 2. Veredicto y estabilidad', '## 5. Memoria, heap', '## 6. Entrada y salida', '## 7. Informes clínicos', '## 10. Conclusiones'):
                self.assertIn(titulo, texto)
            self.assertIn('| PRODUCCION-512M | 250 |', texto)
            self.assertTrue((salida / 'COMPARACION_BATERIA.md').exists())
            filas = (salida / 'comparacion_bateria.csv').read_text(encoding='utf-8').strip().splitlines()
            self.assertEqual(len(filas), 13)
            ops = (salida / 'operaciones_bateria.csv').read_text(encoding='utf-8')
            self.assertIn('reporte_clinico', ops)
            datos = json.loads((salida / 'comparacion_bateria.json').read_text(encoding='utf-8'))
            self.assertEqual(len(datos), 12)

    def test_sin_corridas(self):
        with tempfile.TemporaryDirectory() as t:
            self.assertIn('No se encontraron corridas', cb.generar(t, t))

    def test_io_ausente_no_inventa_cifras(self):
        with tempfile.TemporaryDirectory() as t:
            construir(t, 'CAPACIDAD-2G', 1, 100, io=False, heap_max='1g', limite_mb=2048)
            [(d, p, r, n)] = cb.descubrir(t)
            c = cb.analizar_corrida(d, p, r, n)
            self.assertIsNone(c['io']['backend'])
            self.assertIsNone(c['throttled_pct'])


if __name__ == '__main__':
    unittest.main()
