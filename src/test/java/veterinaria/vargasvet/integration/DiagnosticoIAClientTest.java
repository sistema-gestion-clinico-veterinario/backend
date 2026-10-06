package veterinaria.vargasvet.integration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import veterinaria.vargasvet.integration.DiagnosticoIAClient.ArchivoParte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiagnosticoIAClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private volatile int estado = 200;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/ia/diagnostico", exchange -> {
            cuerpoRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] respuesta = ("data: {\"type\":\"meta\",\"escenario\":\"HC\"}\n\n"
                    + "data: {\"type\":\"chunk\",\"text\":\"Hola\"}\n\n"
                    + "data: {\"type\":\"done\"}\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(estado, respuesta.length);
            exchange.getResponseBody().write(respuesta);
            exchange.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private DiagnosticoIAClient cliente() {
        return new DiagnosticoIAClient("http://127.0.0.1:" + servidor.getAddress().getPort(),
                Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    @Test
    void reenviaLosCamposYLosArchivosComoFormularioYDevuelveLaRespuestaTalCualLlega() {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("motivo_consulta", "Tos desde hace 3 días");
        campos.put("especie", "Perro");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        cliente().transmitir(campos, List.of(
                        new ArchivoParte("archivo_hemograma", "hemograma.pdf", "CONTENIDO-PDF".getBytes(StandardCharsets.UTF_8)),
                        new ArchivoParte("archivo_radiografia", "rx.png", new byte[]{9, 8, 7})),
                salida);

        assertThat(contentType.get()).startsWith("multipart/form-data").contains("boundary=");
        assertThat(cuerpoRecibido.get())
                .contains("name=\"motivo_consulta\"").contains("Tos desde hace 3 días").contains("name=\"especie\"").contains("Perro")
                .contains("name=\"archivo_hemograma\"; filename=\"hemograma.pdf\"").contains("CONTENIDO-PDF")
                .contains("name=\"archivo_radiografia\"; filename=\"rx.png\"");
        assertThat(salida.toString(StandardCharsets.UTF_8))
                .contains("\"type\":\"meta\"").contains("\"text\":\"Hola\"").contains("\"type\":\"done\"");
    }

    @Test
    void siElServicioResponderConErrorSeLanzaYNadaSeCopia() {
        estado = 503;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        assertThatThrownBy(() -> cliente().transmitir(Map.of("motivo_consulta", "x"), List.of(), salida))
                .isInstanceOf(RuntimeException.class);

        assertThat(salida.size()).isZero();
    }

    @Test
    void siElServicioNoEstaDisponibleSeLanzaUnaExcepcion() {
        servidor.stop(0);

        assertThatThrownBy(() -> cliente().transmitir(Map.of("motivo_consulta", "x"), List.of(), new ByteArrayOutputStream()))
                .isInstanceOf(RuntimeException.class);
    }
}
