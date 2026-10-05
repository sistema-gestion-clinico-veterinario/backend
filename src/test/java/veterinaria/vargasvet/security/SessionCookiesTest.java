package veterinaria.vargasvet.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookiesTest {

    private MockHttpServletRequest solicitud(String slug, Cookie... cookies) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (slug != null) {
            request.addHeader(SessionCookies.SLUG_HEADER, slug);
        }
        if (cookies.length > 0) {
            request.setCookies(cookies);
        }
        return request;
    }

    @Test
    void cadaClinicaTieneSusPropiasCookiesYLaDePlataformaConservaLasSuyas() {
        assertThat(SessionCookies.accessName("vargas-vet")).isEqualTo("access_token__vargas-vet");
        assertThat(SessionCookies.refreshName("vargas-vet")).isEqualTo("refresh_token__vargas-vet");
        assertThat(SessionCookies.accessName(null)).isEqualTo("access_token");
        assertThat(SessionCookies.refreshName(null)).isEqualTo("refresh_token");
    }

    @Test
    void elSlugSoloSeAceptaConFormaDeSlug() {
        assertThat(SessionCookies.sanitize(" Vargas-Vet ")).isEqualTo("vargas-vet");
        assertThat(SessionCookies.sanitize("duke-de-can-2")).isEqualTo("duke-de-can-2");
        assertThat(SessionCookies.sanitize("a;b")).isNull();
        assertThat(SessionCookies.sanitize("../x")).isNull();
        assertThat(SessionCookies.sanitize("x=y")).isNull();
        assertThat(SessionCookies.sanitize("-empieza")).isNull();
        assertThat(SessionCookies.sanitize("a".repeat(101))).isNull();
        assertThat(SessionCookies.sanitize("")).isNull();
        assertThat(SessionCookies.sanitize(null)).isNull();
    }

    @Test
    void reconoceCualquierCookieDeSesion() {
        assertThat(SessionCookies.isSessionCookie("access_token")).isTrue();
        assertThat(SessionCookies.isSessionCookie("refresh_token__vargas-vet")).isTrue();
        assertThat(SessionCookies.isSessionCookie("access_token__duke")).isTrue();
        assertThat(SessionCookies.isSessionCookie("caja_dispositivo")).isFalse();
        assertThat(SessionCookies.isSessionCookie("access_tokenx")).isFalse();
        assertThat(SessionCookies.isSessionCookie(null)).isFalse();
    }

    @Test
    void conDosClinicasEnElMismoNavegadorCadaPestanaLeeLaSuya() {
        Cookie a = new Cookie("access_token__clinica-a", "token-a");
        Cookie b = new Cookie("access_token__clinica-b", "token-b");

        assertThat(SessionCookies.readAccess(solicitud("clinica-a", a, b))).contains("token-a");
        assertThat(SessionCookies.readAccess(solicitud("clinica-b", a, b))).contains("token-b");
    }

    @Test
    void unaClinicaSinSesionNoHeredaLaDeOtra() {
        Cookie a = new Cookie("access_token__clinica-a", "token-a");

        assertThat(SessionCookies.readAccess(solicitud("clinica-b", a))).isEmpty();
    }

    @Test
    void conUnaClinicaDeclaradaNuncaSeUsaLaCookieSinSufijoDePlataforma() {
        Cookie plataforma = new Cookie("access_token", "token-plataforma");
        Cookie propia = new Cookie("access_token__clinica-a", "token-a");
        Cookie refrescoPlataforma = new Cookie("refresh_token", "refresco-plataforma");

        assertThat(SessionCookies.readAccess(solicitud("clinica-a", plataforma))).isEmpty();
        assertThat(SessionCookies.readRefresh(solicitud("clinica-a", refrescoPlataforma))).isEmpty();
        assertThat(SessionCookies.readAccess(solicitud("clinica-a", plataforma, propia))).contains("token-a");
    }

    @Test
    void sinClinicaDeclaradaLaCuentaDePlataformaUsaSusCookiesDeSiempre() {
        Cookie plataforma = new Cookie("access_token", "token-plataforma");
        Cookie clinica = new Cookie("access_token__clinica-a", "token-a");

        assertThat(SessionCookies.readAccess(solicitud(null, plataforma))).contains("token-plataforma");
        assertThat(SessionCookies.readAccess(solicitud(null, plataforma, clinica))).contains("token-plataforma");
    }

    @Test
    void sinSlugDeclaradoSoloSeUsaUnaCookieConSufijoSiNoHayAmbiguedad() {
        Cookie a = new Cookie("access_token__clinica-a", "token-a");
        Cookie b = new Cookie("access_token__clinica-b", "token-b");

        assertThat(SessionCookies.readAccess(solicitud(null, a))).contains("token-a");
        assertThat(SessionCookies.readAccess(solicitud(null, a, b))).isEmpty();
    }

    @Test
    void elSlugMalFormadoSeIgnoraYNoElige() {
        Cookie a = new Cookie("access_token__clinica-a", "token-a");

        assertThat(SessionCookies.readAccess(solicitud("clinica-a;x", a))).contains("token-a");
        assertThat(SessionCookies.readAccess(solicitud("clinica-a;x", a, new Cookie("access_token__otra", "t")))).isEmpty();
    }

    @Test
    void laCookieDeRefrescoSeResuelveIgual() {
        Cookie a = new Cookie("refresh_token__clinica-a", "refresco-a");
        Cookie b = new Cookie("refresh_token__clinica-b", "refresco-b");
        Cookie acceso = new Cookie("access_token__clinica-b", "no-es-refresco");

        assertThat(SessionCookies.readRefresh(solicitud("clinica-b", a, b, acceso))).contains("refresco-b");
        assertThat(SessionCookies.readRefresh(solicitud("clinica-a", a, b, acceso))).contains("refresco-a");
    }

    @Test
    void lasCookiesVaciasNoCuentan() {
        assertThat(SessionCookies.readAccess(solicitud("clinica-a", new Cookie("access_token__clinica-a", "")))).isEmpty();
        assertThat(SessionCookies.readAccess(solicitud("clinica-a"))).isEmpty();
    }
}
