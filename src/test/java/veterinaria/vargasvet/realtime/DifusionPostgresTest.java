package veterinaria.vargasvet.realtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class DifusionPostgresTest {

    private final List<DifusionPostgres> creadas = new ArrayList<>();

    @AfterEach
    void cerrar() {
        creadas.forEach(DifusionPostgres::detener);
    }

    private DifusionPostgres difusion(EnvioDeNotificacion envio, int capacidad) {
        DifusionPostgres difusion = new DifusionPostgres(TiempoRealPruebas.conversor(), envio, "canal_prueba", "instancia-a", capacidad);
        creadas.add(difusion);
        return difusion;
    }

    @Test
    void unEventoNormalSeEnviaPorElCanalConSuOrigenDestinoYCuerpo() {
        List<String> canales = new CopyOnWriteArrayList<>();
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> {
            canales.add(canal);
            cargas.add(carga);
        }, 10);

        difusion.difundir("/topic/citas/3", Map.of("tipo", "CITA_CREADA"));

        await().untilAsserted(() -> assertThat(cargas).hasSize(1));
        SobreTiempoReal sobre = SobreTiempoReal.leerEvento(cargas.get(0)).orElseThrow();
        assertThat(canales).containsExactly("canal_prueba");
        assertThat(sobre.origen()).isEqualTo("instancia-a");
        assertThat(sobre.destino()).isEqualTo("/topic/citas/3");
        assertThat(new String(sobre.cuerpo(), StandardCharsets.UTF_8)).isEqualTo("{\"tipo\":\"CITA_CREADA\"}");
    }

    @Test
    void unEventoMayorAlLimiteNoSeEnvia() {
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> cargas.add(carga), 10);

        difusion.difundir("/topic/audit-logs", Map.of("detalle", "x".repeat(DifusionPostgres.LIMITE_BYTES)));
        difusion.detener();

        assertThat(cargas).isEmpty();
        assertThat(difusion.demasiadoGrandes()).isEqualTo(1);
    }

    @Test
    void elLimiteSeMideEnBytesYNoEnCaracteres() {
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> cargas.add(carga), 10);

        difusion.difundir("/topic/audit-logs", Map.of("detalle", "ñ".repeat(DifusionPostgres.LIMITE_BYTES / 2 + 100)));
        difusion.detener();

        assertThat(cargas).isEmpty();
        assertThat(difusion.demasiadoGrandes()).isEqualTo(1);
    }

    @Test
    void siLaBaseEstaLentaLaColaSeLlenaYElLlamadorNuncaEspera() throws Exception {
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicInteger enviados = new AtomicInteger();
        DifusionPostgres difusion = difusion((canal, carga) -> {
            try {
                liberar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            enviados.incrementAndGet();
        }, 2);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (int i = 0; i < 10; i++) {
                difusion.difundir("/topic/citas/1", Map.of("n", i));
            }
        });

        assertThat(difusion.descartados()).isGreaterThanOrEqualTo(7);
        liberar.countDown();
        await().untilAsserted(() -> assertThat(enviados.get()).isEqualTo(10 - (int) difusion.descartados()));
    }

    @Test
    void siElEnvioFallaSeContabilizaYLosSiguientesEventosSiSeEnvian() {
        AtomicInteger intentos = new AtomicInteger();
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> {
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("conexión caída");
            }
            cargas.add(carga);
        }, 10);

        assertThatCode(() -> {
            difusion.difundir("/topic/citas/1", Map.of("n", 1));
            difusion.difundir("/topic/citas/1", Map.of("n", 2));
        }).doesNotThrowAnyException();

        await().untilAsserted(() -> assertThat(cargas).hasSize(1));
        assertThat(difusion.fallidos()).isEqualTo(1);
    }

    @Test
    void losEventosSalenEnElMismoOrdenEnQueSePublicaron() {
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> cargas.add(carga), 100);

        for (int i = 0; i < 50; i++) {
            difusion.difundir("/topic/citas/1", Map.of("n", i));
        }

        await().untilAsserted(() -> assertThat(cargas).hasSize(50));
        for (int i = 0; i < 50; i++) {
            String cuerpo = new String(SobreTiempoReal.leerEvento(cargas.get(i)).orElseThrow().cuerpo(), StandardCharsets.UTF_8);
            assertThat(cuerpo).isEqualTo("{\"n\":" + i + "}");
        }
    }

    @Test
    void alDetenerseSeEnvianLosEventosQueQuedabanEnCola() {
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cargas.add(carga);
        }, 100);

        for (int i = 0; i < 10; i++) {
            difusion.difundir("/topic/citas/1", Map.of("n", i));
        }
        difusion.detener();

        assertThat(cargas).hasSize(10);
    }

    @Test
    void unCuerpoQueNoEsJsonNoSeDifunde() {
        List<String> cargas = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = difusion((canal, carga) -> cargas.add(carga), 10);

        difusion.difundir("/topic/citas/1", "texto plano");
        difusion.detener();

        assertThat(cargas).isEmpty();
    }
}
