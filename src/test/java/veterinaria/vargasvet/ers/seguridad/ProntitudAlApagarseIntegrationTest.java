package veterinaria.vargasvet.ers.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"file:src/main/resources/application.properties", "classpath:aislamiento.properties"},
        properties = "spring.datasource.url=jdbc:h2:mem:vargasvet_prontitud;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
class ProntitudAlApagarseIntegrationTest {

    @Autowired private TestRestTemplate http;
    @Autowired private ApplicationContext contexto;

    @Test
    void alEmpezarElApagadoLaLecturaRechazaTraficoPeroLaVidaSigueRespondiendo() {
        assertThat(codigo("/actuator/health/readiness")).isEqualTo(200);

        try {
            AvailabilityChangeEvent.publish(contexto, ReadinessState.REFUSING_TRAFFIC);

            assertThat(codigo("/actuator/health/readiness")).isEqualTo(503);
            assertThat(codigo("/actuator/health/liveness")).isEqualTo(200);
        } finally {
            AvailabilityChangeEvent.publish(contexto, ReadinessState.ACCEPTING_TRAFFIC);
        }

        assertThat(codigo("/actuator/health/readiness")).isEqualTo(200);
    }

    private int codigo(String ruta) {
        return http.getForEntity(ruta, String.class).getStatusCode().value();
    }
}
