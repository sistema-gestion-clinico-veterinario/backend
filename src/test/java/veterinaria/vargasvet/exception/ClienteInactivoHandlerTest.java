package veterinaria.vargasvet.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.ApiResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteInactivoHandlerTest {

    @Test
    void elRechazoDelRegistroEntregaLoNecesarioParaOfrecerLaReactivacion() {
        ClienteInactivoException error = new ClienteInactivoException(
                "Este cliente fue dado de baja. Para devolverle el acceso usa «Reactivar» en la lista de clientes",
                41L, TipoInactividad.BAJA);

        ResponseEntity<ApiResponse<Map<String, Object>>> respuesta = new GlobalExceptionHandler().handleClienteInactivo(error);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().isSuccess()).isFalse();
        assertThat(respuesta.getBody().getMessage()).contains("dado de baja");
        assertThat(respuesta.getBody().getData())
                .containsEntry("code", "CLIENTE_INACTIVO")
                .containsEntry("apoderadoId", 41L)
                .containsEntry("tipoInactividad", "BAJA");
    }

    @Test
    void esUnaIllegalArgumentExceptionParaQuienYaLaTratabaAsi() {
        assertThat(new ClienteInactivoException("x", 1L, TipoInactividad.SUSPENSION))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
