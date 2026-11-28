package veterinaria.vargasvet.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import veterinaria.vargasvet.realtime.DifusionEntreInstancias;
import veterinaria.vargasvet.realtime.EscuchaPostgres;
import veterinaria.vargasvet.realtime.PublicadorTiempoReal;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DifusionTiempoRealConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(DifusionTiempoRealConfig.class)
            .withBean(SimpMessagingTemplate.class, () -> mock(SimpMessagingTemplate.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PublicadorTiempoReal.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/ninguna",
                    "spring.datasource.username=usuario",
                    "spring.datasource.password=clave");

    @Test
    void porDefectoNoHayDifusionNiEscucha() {
        contexto.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(DifusionEntreInstancias.class);
            assertThat(ctx).doesNotHaveBean(EscuchaPostgres.class);
            assertThat(ctx).hasSingleBean(PublicadorTiempoReal.class);
        });
    }

    @Test
    void conLaBanderaActivaHayDifusionYEscucha() {
        contexto.withPropertyValues("app.realtime.distribuido=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(DifusionEntreInstancias.class);
            assertThat(ctx).hasSingleBean(EscuchaPostgres.class);
            assertThat(ctx).hasSingleBean(PublicadorTiempoReal.class);
            assertThat(ctx.getBean(EscuchaPostgres.class).estado()).isNotEqualTo(EscuchaPostgres.Estado.ESCUCHANDO);
        });
    }

    @Test
    void conLaBanderaApagadaElPublicadorSoloEntregaLocalmente() {
        contexto.run(ctx -> {
            SimpMessagingTemplate mensajeria = ctx.getBean(SimpMessagingTemplate.class);

            ctx.getBean(PublicadorTiempoReal.class).publicar("/topic/caja/1", Map.of("n", 1));

            org.mockito.Mockito.verify(mensajeria).convertAndSend("/topic/caja/1", Map.of("n", 1));
        });
    }
}
