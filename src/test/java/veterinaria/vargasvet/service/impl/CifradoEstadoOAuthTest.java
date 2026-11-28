package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradoEstadoOAuthTest {

    private static final String LLAVE_JWT = "classpath:keys/e2e_private_key.pem";

    private static byte[] clave(int semilla) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) semilla);
        return bytes;
    }

    @Test
    void cifraYDescifraConElMismoContexto() {
        CifradoEstadoOAuth cifrado = new CifradoEstadoOAuth(clave(1));

        String contenido = cifrado.cifrar("{\"slug\":\"vargas-vet\"}", "INTENT:abc");

        assertThat(contenido).doesNotContain("vargas-vet");
        assertThat(cifrado.descifrar(contenido, "INTENT:abc")).isEqualTo("{\"slug\":\"vargas-vet\"}");
    }

    @Test
    void elMismoTextoNuncaSeCifraDosVecesIgual() {
        CifradoEstadoOAuth cifrado = new CifradoEstadoOAuth(clave(1));

        assertThat(cifrado.cifrar("a", "x")).isNotEqualTo(cifrado.cifrar("a", "x"));
    }

    @Test
    void unContenidoMovidoAOtraFilaNoSeDescifra() {
        CifradoEstadoOAuth cifrado = new CifradoEstadoOAuth(clave(1));
        String contenido = cifrado.cifrar("secreto", "SESION:uno");

        assertThatThrownBy(() -> cifrado.descifrar(contenido, "SESION:dos")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unContenidoAlteradoOCortoNoSeDescifra() {
        CifradoEstadoOAuth cifrado = new CifradoEstadoOAuth(clave(1));
        byte[] bytes = Base64.getDecoder().decode(cifrado.cifrar("secreto", "c"));
        bytes[bytes.length - 1] ^= 1;

        assertThatThrownBy(() -> cifrado.descifrar(Base64.getEncoder().encodeToString(bytes), "c"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cifrado.descifrar("AAAA", "c")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void otraClaveNoDescifra() {
        String contenido = new CifradoEstadoOAuth(clave(1)).cifrar("secreto", "c");

        assertThatThrownBy(() -> new CifradoEstadoOAuth(clave(2)).descifrar(contenido, "c"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sinClaveConfiguradaTodasLasInstanciasDerivanLaMismaDeLaLlaveJwt() throws Exception {
        DefaultResourceLoader cargador = new DefaultResourceLoader();
        CifradoEstadoOAuth instanciaA = new CifradoEstadoOAuth("", LLAVE_JWT, cargador);
        CifradoEstadoOAuth instanciaB = new CifradoEstadoOAuth("  ", LLAVE_JWT, cargador);
        CifradoEstadoOAuth instanciaC = new CifradoEstadoOAuth(null, LLAVE_JWT, cargador);

        String contenido = instanciaA.cifrar("secreto", "c");

        assertThat(instanciaB.descifrar(contenido, "c")).isEqualTo("secreto");
        assertThat(instanciaC.descifrar(contenido, "c")).isEqualTo("secreto");
    }

    @Test
    void laClaveConfiguradaTienePrioridadSobreLaDerivada() throws Exception {
        DefaultResourceLoader cargador = new DefaultResourceLoader();
        String configurada = Base64.getEncoder().encodeToString(clave(9));
        String contenido = new CifradoEstadoOAuth(configurada, LLAVE_JWT, cargador).cifrar("secreto", "c");

        assertThat(new CifradoEstadoOAuth(clave(9)).descifrar(contenido, "c")).isEqualTo("secreto");
        assertThatThrownBy(() -> new CifradoEstadoOAuth("", LLAVE_JWT, cargador).descifrar(contenido, "c"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaClaveConfiguradaInvalidaImpideArrancarConUnMensajeClaro() {
        DefaultResourceLoader cargador = new DefaultResourceLoader();

        assertThatThrownBy(() -> new CifradoEstadoOAuth("no es base64!", LLAVE_JWT, cargador))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("openssl rand -base64 32");
        assertThatThrownBy(() -> new CifradoEstadoOAuth(Base64.getEncoder().encodeToString(new byte[16]), LLAVE_JWT, cargador))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }
}
