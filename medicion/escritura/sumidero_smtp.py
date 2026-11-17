import asyncio
import sys
import time

PUERTO = 2525
recibidos = 0


async def atender(lector, escritor):
    global recibidos
    escritor.write(b'220 sumidero ESMTP\r\n')
    await escritor.drain()
    en_datos = False
    try:
        while True:
            linea = await lector.readline()
            if not linea:
                break
            texto = linea.decode('utf-8', errors='replace').rstrip('\r\n')
            if en_datos:
                if texto == '.':
                    en_datos = False
                    recibidos += 1
                    print(f'MSG {recibidos} {time.time():.0f}', flush=True)
                    escritor.write(b'250 2.0.0 recibido\r\n')
                    await escritor.drain()
                continue
            orden = texto.upper()
            if orden.startswith('EHLO'):
                escritor.write(b'250-sumidero\r\n250 8BITMIME\r\n')
            elif orden.startswith('HELO'):
                escritor.write(b'250 sumidero\r\n')
            elif orden.startswith('DATA'):
                en_datos = True
                escritor.write(b'354 adelante\r\n')
            elif orden.startswith('QUIT'):
                escritor.write(b'221 adios\r\n')
                await escritor.drain()
                break
            elif orden.startswith(('MAIL', 'RCPT', 'RSET', 'NOOP', 'VRFY')):
                escritor.write(b'250 ok\r\n')
            else:
                escritor.write(b'502 no implementado\r\n')
            await escritor.drain()
    except (ConnectionError, asyncio.IncompleteReadError):
        pass
    finally:
        escritor.close()


async def principal():
    servidor = await asyncio.start_server(atender, '0.0.0.0', PUERTO)
    print(f'LISTO {PUERTO}', flush=True)
    async with servidor:
        await servidor.serve_forever()


if __name__ == '__main__':
    try:
        asyncio.run(principal())
    except KeyboardInterrupt:
        sys.exit(0)
