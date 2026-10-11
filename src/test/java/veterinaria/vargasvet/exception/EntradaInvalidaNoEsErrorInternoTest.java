package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EntradaInvalidaNoEsErrorInternoTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();

    @Test
    void unCuerpoConFormatoIncorrectoEsUnaSolicitudInvalida() {
        var respuesta = manejador.handleBodyNotReadable(
                new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class)));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody().getMessage()).contains("formato esperado").doesNotContain("JSON");
    }

    @Test
    void unaSolicitudSinArchivoMultipartEsUnaSolicitudInvalida() {
        var respuesta = manejador.handleMultipart(new MultipartException("Current request is not a multipart request"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void unIdentificadorConTipoIncorrectoEsUnaSolicitudInvalida() throws Exception {
        MethodParameter parametro = new MethodParameter(
                EntradaInvalidaNoEsErrorInternoTest.class.getDeclaredMethod("ejemplo", Long.class), 0);
        var respuesta = manejador.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", parametro, new NumberFormatException()));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody().getMessage()).contains("'id'");
    }

    @Test
    void unaExcepcionRealmenteInesperadaSigueSiendoErrorInternoSinDetalles() {
        var respuesta = manejador.handleRuntimeException(
                new RuntimeException("detalle interno secreto"), mock(jakarta.servlet.http.HttpServletRequest.class));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(500);
        assertThat(respuesta.getBody().getMessage()).doesNotContain("secreto");
    }

    @SuppressWarnings("unused")
    private void ejemplo(Long id) {
    }
}
