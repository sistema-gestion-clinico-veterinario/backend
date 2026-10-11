package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import veterinaria.vargasvet.config.WebSecurityConfig;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordEncoderConfigurationTest {

    private final WebSecurityConfig configuracion = Mockito.mock(WebSecurityConfig.class, Mockito.CALLS_REAL_METHODS);

    @Test
    void elValorOperativoPorDefectoEs12() throws Exception {
        Properties propiedades = new Properties();
        try (InputStream entrada = new FileInputStream("src/main/resources/application.properties")) {
            propiedades.load(entrada);
        }

        assertThat(propiedades.getProperty("security.bcrypt-strength")).isEqualTo("${BCRYPT_STRENGTH:12}");
    }

    @Test
    void conElValorOperativoLasClavesSeGuardanConBCryptDeCosto12() {
        PasswordEncoder codificador = configuracion.passwordEncoder(12);

        String hash = codificador.encode("Frase extensa de prueba 2026");

        assertThat(hash).matches("^\\$2a\\$12\\$[./A-Za-z0-9]{53}$").doesNotContain("Frase extensa");
        assertThat(codificador.matches("Frase extensa de prueba 2026", hash)).isTrue();
        assertThat(codificador.matches("otra frase", hash)).isFalse();
    }

    @Test
    void elFactorDeCostoEsConfigurable() {
        assertThat(configuracion.passwordEncoder(10).encode("x")).startsWith("$2a$10$");
        assertThat(configuracion.passwordEncoder(13).encode("x")).startsWith("$2a$13$");
    }

    @Test
    void unCostoFueraDelRangoSeguroImpideArrancar() {
        assertThatThrownBy(() -> configuracion.passwordEncoder(9)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuracion.passwordEncoder(15)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unHashDeMenorCostoSeDetectaParaVolverAGuardarloAlIniciarSesion() {
        PasswordEncoder codificador = configuracion.passwordEncoder(12);
        String antiguo = new BCryptPasswordEncoder(10).encode("Frase extensa de prueba 2026");

        assertThat(codificador.matches("Frase extensa de prueba 2026", antiguo)).isTrue();
        assertThat(codificador.upgradeEncoding(antiguo)).isTrue();
        assertThat(codificador.upgradeEncoding(codificador.encode("Frase extensa de prueba 2026"))).isFalse();
    }
}
