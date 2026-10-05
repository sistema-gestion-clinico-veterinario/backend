package veterinaria.vargasvet.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CajaDispositivoCookieTest {

    private final CajaDispositivoCookie cookie = new CajaDispositivoCookie();

    @Test
    void cadaSedeTieneSuPropiaCookieYUnSlugRaroNuncaLlegaAlNombre() {
        assertThat(CajaDispositivoCookie.nombre("sede-a")).isEqualTo("caja_dispositivo__sede-a");
        assertThat(CajaDispositivoCookie.nombre(null)).isEqualTo("caja_dispositivo");
        assertThat(CajaDispositivoCookie.nombre("a;b=c")).isEqualTo("caja_dispositivo");
    }

    @Test
    void viajaATodaLaApiPorqueLosCobrosNoEstanBajoLaRutaDeLaCaja() {
        ReflectionTestUtils.setField(cookie, "cookieSecure", true);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        cookie.fijar(respuesta, "valor-aleatorio", "sede-a");

        assertThat(respuesta.getHeader("Set-Cookie"))
                .startsWith("caja_dispositivo__sede-a=valor-aleatorio;")
                .contains("Path=/api/v1;")
                .contains("HttpOnly").contains("Secure").contains("SameSite=Strict");
    }

    @Test
    void cadaSedeLeeSoloLaSuya() {
        MockHttpServletRequest solicitud = new MockHttpServletRequest();
        solicitud.setCookies(new Cookie("caja_dispositivo__sede-a", "valor-a"), new Cookie("caja_dispositivo__sede-b", "valor-b"),
                new Cookie("caja_dispositivo", "valor-viejo"));

        assertThat(cookie.leer(solicitud, "sede-a")).contains("valor-a");
        assertThat(cookie.leer(solicitud, "sede-b")).contains("valor-b");
        assertThat(cookie.leer(solicitud, "sede-c")).isEmpty();
    }

    @Test
    void borrarSoloBorraLaCookieDeEsaSede() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        cookie.borrar(respuesta, "sede-a");

        assertThat(respuesta.getHeader("Set-Cookie")).startsWith("caja_dispositivo__sede-a=;").contains("Max-Age=0");
    }
}
