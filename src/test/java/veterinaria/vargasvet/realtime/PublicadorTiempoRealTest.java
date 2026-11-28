package veterinaria.vargasvet.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.realtime.TiempoRealPruebas.CanalCapturador;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

class PublicadorTiempoRealTest {

    private static Map<String, Object> evento() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo", "CUENTA_PREVENTIVA_CREADA");
        cuerpo.put("mascotaNombre", "Niño \"Rex\"");
        cuerpo.put("total", new BigDecimal("1234.50"));
        cuerpo.put("fecha", LocalDateTime.of(2026, 11, 28, 9, 30, 15));
        return cuerpo;
    }

    @Test
    void sinDifusionSoloEntregaALosClientesDeEstaInstancia() {
        CanalCapturador canal = new CanalCapturador();
        PublicadorTiempoReal publicador = new PublicadorTiempoReal(TiempoRealPruebas.plantilla(canal), Optional.empty());

        publicador.publicar("/topic/caja/7", evento());

        assertThat(canal.mensajes).hasSize(1);
        assertThat(SimpMessageHeaderAccessor.getDestination(canal.mensajes.get(0).getHeaders())).isEqualTo("/topic/caja/7");
    }

    @Test
    void conDifusionLaOtraInstanciaEntregaExactamenteLoMismoQueLaPrimera() {
        CanalCapturador canalA = new CanalCapturador();
        SimpMessagingTemplate plantillaA = TiempoRealPruebas.plantilla(canalA);
        List<String> notificaciones = new CopyOnWriteArrayList<>();
        DifusionPostgres difusion = new DifusionPostgres(plantillaA.getMessageConverter(),
                (canal, carga) -> notificaciones.add(carga), "canal_prueba", "instancia-a", 10);
        PublicadorTiempoReal publicadorA = new PublicadorTiempoReal(plantillaA, Optional.of(difusion));

        publicadorA.publicar("/topic/caja/7", evento());

        await().untilAsserted(() -> assertThat(notificaciones).hasSize(1));
        CanalCapturador canalB = new CanalCapturador();
        SobreTiempoReal sobre = SobreTiempoReal.leerEvento(notificaciones.get(0)).orElseThrow();
        new EntregaLocalTiempoReal(TiempoRealPruebas.plantilla(canalB)).accept(sobre);

        Message<?> local = canalA.mensajes.get(0);
        Message<?> remoto = canalB.mensajes.get(0);
        assertThat(sobre.origen()).isEqualTo("instancia-a");
        assertThat((byte[]) remoto.getPayload()).isEqualTo((byte[]) local.getPayload());
        assertThat(SimpMessageHeaderAccessor.getDestination(remoto.getHeaders()))
                .isEqualTo(SimpMessageHeaderAccessor.getDestination(local.getHeaders()));
        assertThat(String.valueOf(remoto.getHeaders().get(MessageHeaders.CONTENT_TYPE)))
                .isEqualTo(String.valueOf(local.getHeaders().get(MessageHeaders.CONTENT_TYPE)));
        difusion.detener();
    }

    @Test
    void siLaDifusionFallaLosClientesLocalesIgualRecibenElEvento() {
        CanalCapturador canal = new CanalCapturador();
        DifusionEntreInstancias rota = (destino, cuerpo) -> {
            throw new IllegalStateException("sin base");
        };
        PublicadorTiempoReal publicador = new PublicadorTiempoReal(TiempoRealPruebas.plantilla(canal), Optional.of(rota));

        assertThatCode(() -> publicador.publicar("/topic/caja/7", evento())).doesNotThrowAnyException();

        assertThat(canal.mensajes).hasSize(1);
    }
}
