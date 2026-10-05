package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispositivoInfoTest {

    @Test
    void describeElNavegadorYElSistemaSinDatosPersonales() {
        assertThat(DispositivoInfo.describir(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"))
                .isEqualTo("Chrome en Windows");
        assertThat(DispositivoInfo.describir(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36 Edg/120.0"))
                .isEqualTo("Edge en Windows");
        assertThat(DispositivoInfo.describir("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15"))
                .isEqualTo("Safari en macOS");
        assertThat(DispositivoInfo.describir("Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0"))
                .isEqualTo("Firefox en Linux");
    }

    @Test
    void sinDatosNoInventaNada() {
        assertThat(DispositivoInfo.describir(null)).isEqualTo("Equipo sin identificar");
        assertThat(DispositivoInfo.describir("  ")).isEqualTo("Equipo sin identificar");
        assertThat(DispositivoInfo.describir("curl/8.0")).isEqualTo("Navegador en sistema desconocido");
    }
}
