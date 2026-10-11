package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceededHandlerTest {

    @Test
    void responde429ConLaVentanaDeLaPoliticaQueFueExcedida() {
        var response = new GlobalExceptionHandler().handleRateLimitExceeded(
                new RateLimitExceededException(Duration.ofHours(1)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("3600");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
