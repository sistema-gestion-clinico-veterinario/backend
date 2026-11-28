package veterinaria.vargasvet.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class DifusionPostgres implements DifusionEntreInstancias {

    static final int LIMITE_BYTES = 7500;

    private final MessageConverter conversor;
    private final EnvioDeNotificacion envio;
    private final String canal;
    private final String origen;
    private final ThreadPoolExecutor ejecutor;
    private final AtomicLong descartados = new AtomicLong();
    private final AtomicLong demasiadoGrandes = new AtomicLong();
    private final AtomicLong fallidos = new AtomicLong();

    public DifusionPostgres(MessageConverter conversor, EnvioDeNotificacion envio, String canal, String origen,
                            int capacidadCola) {
        this.conversor = conversor;
        this.envio = envio;
        this.canal = canal;
        this.origen = origen;
        this.ejecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacidadCola),
                tarea -> {
                    Thread hilo = new Thread(tarea, "tiempo-real-difusion");
                    hilo.setDaemon(true);
                    return hilo;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void difundir(String destino, Object cuerpo) {
        try {
            byte[] json = serializar(cuerpo);
            if (json == null) {
                return;
            }
            String carga = SobreTiempoReal.evento(origen, destino, json);
            if (carga.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES) {
                long total = demasiadoGrandes.incrementAndGet();
                log.warn("Evento de tiempo real demasiado grande para las demás instancias (destino={}, total={})",
                        destino, total);
                return;
            }
            ejecutor.execute(() -> notificar(carga));
        } catch (RejectedExecutionException e) {
            long total = descartados.incrementAndGet();
            log.warn("Cola de difusión de tiempo real llena; evento descartado para las demás instancias (total={})", total);
        } catch (Exception e) {
            log.warn("No se pudo preparar el evento de tiempo real para las demás instancias: {}", e.getClass().getSimpleName());
        }
    }

    private byte[] serializar(Object cuerpo) {
        Message<?> mensaje = conversor.toMessage(cuerpo, null);
        if (mensaje == null || !(mensaje.getPayload() instanceof byte[] bytes)) {
            log.warn("El evento de tiempo real no se pudo convertir a JSON; no se difunde");
            return null;
        }
        Object tipo = mensaje.getHeaders().get(MessageHeaders.CONTENT_TYPE);
        if (tipo != null && !esJson(tipo)) {
            log.warn("El evento de tiempo real no es JSON; no se difunde");
            return null;
        }
        return bytes;
    }

    private static boolean esJson(Object tipo) {
        MimeType mime = tipo instanceof MimeType valor ? valor : MimeType.valueOf(tipo.toString());
        return MimeTypeUtils.APPLICATION_JSON.isCompatibleWith(mime);
    }

    private void notificar(String carga) {
        try {
            envio.enviar(canal, carga);
        } catch (Exception e) {
            long total = fallidos.incrementAndGet();
            log.warn("No se pudo notificar a las demás instancias (total={}): {}", total, e.getClass().getSimpleName());
        }
    }

    public long descartados() {
        return descartados.get();
    }

    public long demasiadoGrandes() {
        return demasiadoGrandes.get();
    }

    public long fallidos() {
        return fallidos.get();
    }

    public void detener() {
        ejecutor.shutdown();
        try {
            if (!ejecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                ejecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ejecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
