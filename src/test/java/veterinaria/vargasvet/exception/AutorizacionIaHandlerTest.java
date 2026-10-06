package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AutorizacionIaHandlerTest {

    @Test
    void respondeProhibidoConElCodigoQueLaPantallaNecesita() {
        var respuesta = new GlobalExceptionHandler().handleAutorizacionIa(new AutorizacionIaRequeridaException(5L));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().isSuccess()).isFalse();
        assertThat(respuesta.getBody().getMessage()).contains("no autorizó el uso de inteligencia artificial");
        assertThat(respuesta.getBody().getData()).containsEntry("code", "IA_SIN_AUTORIZACION")
                .containsEntry("apoderadoId", 5L);
    }
}
