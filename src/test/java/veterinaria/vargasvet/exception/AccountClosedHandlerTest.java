package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountClosedHandlerTest {

    @Test
    void respondeConConflictoYElCodigoQueLaPantallaNecesitaParaOfrecerReactivar() {
        LocalDateTime hasta = LocalDateTime.of(2026, 11, 5, 13, 38);

        var respuesta = new GlobalExceptionHandler().handleAccountClosed(new AccountClosedException(10, 7, hasta));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().isSuccess()).isFalse();
        assertThat(respuesta.getBody().getData()).containsEntry("code", "CUENTA_CERRADA")
                .containsEntry("reactivableHasta", "2026-11-05T13:38")
                .doesNotContainKeys("usuarioId", "companyId");
    }
}
