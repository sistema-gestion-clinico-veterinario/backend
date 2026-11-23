package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;

class IndisponibilidadDeBaseDeDatosTest {

    @Test
    void reconoceLosFallosDeConexionYDeTransaccion() {
        assertThat(IndisponibilidadDeBaseDeDatos.es(new CannotCreateTransactionException("sin conexión"))).isTrue();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new DataAccessResourceFailureException("conexión cerrada"))).isTrue();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new QueryTimeoutException("tiempo agotado"))).isTrue();
    }

    @Test
    void reconoceUnFalloDeConexionEscondidoEnLaCausa() {
        RuntimeException envuelta = new IllegalStateException("no se pudo guardar",
                new RuntimeException("capa intermedia",
                        new SQLTransientConnectionException("HikariPool-1 - Connection is not available")));

        assertThat(IndisponibilidadDeBaseDeDatos.es(envuelta)).isTrue();
    }

    @Test
    void noConfundeUnaSesionInvalidaNiUnErrorDeDatosConUnaCaida() {
        assertThat(IndisponibilidadDeBaseDeDatos.es(new BadCredentialsException("sesión inválida"))).isFalse();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new DataIntegrityViolationException("clave duplicada"))).isFalse();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new IllegalArgumentException("dato inválido"))).isFalse();
        assertThat(IndisponibilidadDeBaseDeDatos.es(null)).isFalse();
    }

    @Test
    void unaCadenaDeCausasCircularNoLoDejaEnUnBucle() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertThat(IndisponibilidadDeBaseDeDatos.es(a)).isFalse();
    }
}
