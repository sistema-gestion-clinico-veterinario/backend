package veterinaria.vargasvet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Protección contra vaciar una base que no es de pruebas")
class BaseDesechableDePruebasTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5432/prueba_flyway",
            "jdbc:postgresql://127.0.0.1:54398/flyway_test",
            "jdbc:postgresql://[::1]:5432/test_migraciones",
            "jdbc:postgresql://LOCALHOST/Prueba?user=postgres"
    })
    void aceptaUnaBaseLocalConNombreDePrueba(String url) {
        assertThatCode(() -> BaseDesechableDePruebas.exigir(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres",
            "jdbc:postgresql://db.ejemplo.supabase.co:5432/prueba_flyway",
            "jdbc:postgresql://10.0.0.5:5432/prueba_flyway",
            "jdbc:postgresql://localhost.evil.com:5432/prueba_flyway"
    })
    void rechazaCualquierServidorQueNoSeaLocal(String url) {
        assertThatThrownBy(() -> BaseDesechableDePruebas.exigir(url))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("servidor local");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5432/postgres",
            "jdbc:postgresql://localhost:5432/vetsystem",
            "jdbc:postgresql://localhost/produccion?sslmode=require"
    })
    void rechazaUnaBaseLocalSinNombreDePrueba(String url) {
        assertThatThrownBy(() -> BaseDesechableDePruebas.exigir(url))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prueba");
    }

    @Test
    void rechazaDireccionesAusentesOQueNoSonDePostgres() {
        assertThatThrownBy(() -> BaseDesechableDePruebas.exigir(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> BaseDesechableDePruebas.exigir("jdbc:h2:mem:prueba")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> BaseDesechableDePruebas.exigir("")).isInstanceOf(IllegalStateException.class);
    }
}
