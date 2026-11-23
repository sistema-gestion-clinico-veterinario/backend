package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.CannotCreateTransactionException;
import veterinaria.vargasvet.dto.ApiResponse;

import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerBaseCaidaTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<ApiResponse<Void>> manejar(RuntimeException error) {
        return handler.handleRuntimeException(error, new MockHttpServletRequest("POST", "/api/v1/payments"));
    }

    @Test
    void unaBaseNoDisponibleSeResponde503ConReintento() {
        ResponseEntity<ApiResponse<Void>> respuesta =
                manejar(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(503);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isEqualTo("5");
        assertThat(respuesta.getBody().getMessage()).contains("temporalmente");
    }

    @Test
    void unFalloDeConexionEnvueltoEnOtraExcepcionTambienEs503() {
        ResponseEntity<ApiResponse<Void>> respuesta = manejar(new IllegalStateException("no se pudo cobrar",
                new SQLTransientConnectionException("HikariPool-1 - Connection is not available")));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void unErrorDeDatosSigueSiendoUn500() {
        ResponseEntity<ApiResponse<Void>> respuesta =
                manejar(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isNull();
    }
}
