package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CodeResendTooSoonHandlerTest {

    @Test
    void respondeConLosSegundosQueFaltanTantoEnElCuerpoComoEnRetryAfter() {
        var respuesta = new GlobalExceptionHandler().handleCodeResendTooSoon(new CodeResendTooSoonException(42));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isEqualTo("42");
        assertThat(respuesta.getBody().isSuccess()).isFalse();
        assertThat(respuesta.getBody().getMessage()).isEqualTo("Espera 42 segundos antes de pedir otro código.");
        assertThat(respuesta.getBody().getData()).containsEntry("retryAfterSeconds", 42L);
    }
}
