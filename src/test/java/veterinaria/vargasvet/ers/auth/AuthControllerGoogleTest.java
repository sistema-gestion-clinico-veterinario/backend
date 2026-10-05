package veterinaria.vargasvet.ers.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.controller.AuthController;
import veterinaria.vargasvet.dto.request.GoogleExchangeRequest;
import veterinaria.vargasvet.dto.request.GoogleIntentRequest;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.exception.GoogleAccountSuspendedException;
import veterinaria.vargasvet.exception.GoogleClinicAccessException;
import veterinaria.vargasvet.exception.GoogleEmailMismatchException;
import veterinaria.vargasvet.service.EmailChangeService;
import veterinaria.vargasvet.service.UsuarioService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Inicio de sesión y activación con Google: el navegador queda ligado al flujo por una cookie y un
 * "state" aleatorio, y ni la clínica ni el token de invitación viajan por la URL que pasa por Google.
 */
class AuthControllerGoogleTest {

    private static final String FRONTEND = "https://app.test";

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final GoogleOAuthService googleOAuthService = mock(GoogleOAuthService.class);
    private final GoogleLoginExchangeStore exchangeStore = mock(GoogleLoginExchangeStore.class);
    private final GoogleOAuthFlowStore flowStore = new GoogleOAuthFlowStore();
    private final AuthController controller = new AuthController(
            usuarioService, mock(EmailChangeService.class), googleOAuthService, exchangeStore, flowStore,
            mock(veterinaria.vargasvet.service.AccountClosureService.class),
                org.mockito.Mockito.mock(veterinaria.vargasvet.security.ClientIpResolver.class));

    private String estadoEnviadoAGoogle;
    private String desafioEnviadoAGoogle;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Strict");
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND);
        ReflectionTestUtils.setField(controller, "accessTokenMaxAge", 1800L);
        ReflectionTestUtils.setField(controller, "refreshTokenMaxAge", 604800L);
        when(googleOAuthService.buildAuthorizationUrl(anyString(), anyString())).thenAnswer(invocation -> {
            estadoEnviadoAGoogle = invocation.getArgument(0);
            desafioEnviadoAGoogle = invocation.getArgument(1);
            return "https://accounts.google.test/auth?state=" + estadoEnviadoAGoogle;
        });
    }

    private String intencion(String slug, String activationToken) {
        GoogleIntentRequest request = new GoogleIntentRequest();
        request.setSlug(slug);
        request.setActivationToken(activationToken);
        return controller.googleIntent(request).getBody().getData().get("intent");
    }

    private MockHttpServletResponse iniciar(String intent) {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        controller.googleStart(intent, respuesta);
        return respuesta;
    }

    private String destino(org.springframework.http.ResponseEntity<Void> respuesta) {
        return respuesta.getHeaders().getLocation().toString();
    }

    private org.springframework.http.ResponseEntity<Void> volverDeGoogle(String code, String state, String cookie) {
        return controller.googleCallback(code, state, null, cookie, new MockHttpServletResponse());
    }

    private String empezarLogin() {
        iniciar(intencion("Vargas-Vet", null));
        return estadoEnviadoAGoogle;
    }

    @Test
    @DisplayName("La intención guarda la clínica en el servidor y devuelve solo un código opaco")
    void laIntencionDevuelveUnCodigoOpaco() {
        String intent = intencion("Vargas-Vet", null);

        assertThat(intent).isNotBlank().doesNotContain("vargas");
        assertThat(flowStore.consumeIntent(intent).slug()).isEqualTo("vargas-vet");
    }

    @Test
    void sinClinicaNiEnlaceDeActivacionNoSeRegistraLaIntencion() {
        assertThatThrownBy(() -> intencion(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> intencion("  ", "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSeAceptaClinicaYEnlaceDeActivacionAlMismoTiempo() {
        assertThatThrownBy(() -> intencion("vargas-vet", "token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("El inicio fija una cookie HttpOnly con el state y manda a Google con state aleatorio y PKCE")
    void elInicioFijaLaCookieYEnviaAGoogle() {
        MockHttpServletResponse respuesta = iniciar(intencion("vargas-vet", null));

        String cookie = respuesta.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("google_oauth_state=" + estadoEnviadoAGoogle)
                .contains("HttpOnly").contains("Secure").contains("SameSite=Lax")
                .contains("Path=/api/v1/auth/google").contains("Max-Age=600");
        assertThat(estadoEnviadoAGoogle).hasSizeGreaterThanOrEqualTo(43);
        assertThat(desafioEnviadoAGoogle).isNotBlank();
        assertThat(estadoEnviadoAGoogle).doesNotContain("vargas");
    }

    @Test
    void laCookieDelStateSigueLaxAunqueLasDeSesionSeanStrict() {
        MockHttpServletResponse respuesta = iniciar(intencion("vargas-vet", null));

        assertThat(respuesta.getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Lax").doesNotContain("Strict");
    }

    @Test
    void unaIntencionSoloIniciaUnaVez() {
        String intent = intencion("vargas-vet", null);
        iniciar(intent);

        MockHttpServletResponse segunda = new MockHttpServletResponse();
        var respuesta = controller.googleStart(intent, segunda);

        assertThat(destino(respuesta)).isEqualTo(FRONTEND + "/login?authError=google_fallo");
        assertThat(segunda.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void sinIntencionValidaVuelveAlLoginSinTocarGoogle() {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        var redireccion = controller.googleStart("desconocida", respuesta);

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/login?authError=google_fallo");
        verify(googleOAuthService, never()).buildAuthorizationUrl(anyString(), anyString());
    }

    @Test
    @DisplayName("El retorno completo: valida el state con la cookie, canjea con el verificador PKCE y entra a la clínica del contexto")
    void elRetornoCompletoEntraALaClinicaDelContexto() {
        String state = empezarLogin();
        when(googleOAuthService.resolveIdentity(eq("codigo-google"), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", true));
        AuthResponse sesion = new AuthResponse();
        when(usuarioService.loginWithGoogle("ana@example.test", "vargas-vet")).thenReturn(sesion);
        when(exchangeStore.store(sesion)).thenReturn("canje-1");
        MockHttpServletResponse respuestaHttp = new MockHttpServletResponse();

        var redireccion = controller.googleCallback("codigo-google", state, null, state, respuestaHttp);

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/auth/google/callback?code=canje-1");
        ArgumentCaptor<String> verificador = ArgumentCaptor.forClass(String.class);
        verify(googleOAuthService).resolveIdentity(eq("codigo-google"), verificador.capture());
        assertThat(GoogleOAuthService.codeChallenge(verificador.getValue())).isEqualTo(desafioEnviadoAGoogle);
        assertThat(respuestaHttp.getHeader(HttpHeaders.SET_COOKIE)).startsWith("google_oauth_state=;").contains("Max-Age=0");
    }

    @Test
    @DisplayName("Login CSRF: un retorno sin la cookie del navegador se rechaza antes de hablar con Google")
    void unRetornoSinLaCookieSeRechaza() {
        String stateDelAtacante = empezarLogin();

        var redireccion = volverDeGoogle("codigo-del-atacante", stateDelAtacante, null);

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/login?authError=google_fallo");
        verifyNoInteractions(usuarioService, exchangeStore);
        verify(googleOAuthService, never()).resolveIdentity(anyString(), anyString());
    }

    @Test
    void unRetornoConUnaCookieDeOtroFlujoSeRechazaYNoGastaElFlujoLegitimo() {
        String state = empezarLogin();

        var intruso = volverDeGoogle("codigo", state, "otro-estado");
        assertThat(destino(intruso)).isEqualTo(FRONTEND + "/login?authError=google_fallo");

        when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", true));
        when(usuarioService.loginWithGoogle(anyString(), anyString())).thenReturn(new AuthResponse());
        when(exchangeStore.store(any())).thenReturn("canje-2");
        var legitimo = volverDeGoogle("codigo", state, state);
        assertThat(destino(legitimo)).isEqualTo(FRONTEND + "/auth/google/callback?code=canje-2");
    }

    @Test
    void elMismoRetornoNoSePuedeRepetir() {
        String state = empezarLogin();
        when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", true));
        when(usuarioService.loginWithGoogle(anyString(), anyString())).thenReturn(new AuthResponse());
        when(exchangeStore.store(any())).thenReturn("canje-3");
        volverDeGoogle("codigo", state, state);

        var repetido = volverDeGoogle("codigo", state, state);

        assertThat(destino(repetido)).isEqualTo(FRONTEND + "/login?authError=google_fallo");
        verify(usuarioService).loginWithGoogle(anyString(), anyString());
    }

    @Test
    void sinStateNoSeAtiendeElRetorno() {
        var redireccion = volverDeGoogle("codigo", null, "cualquiera");

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/login?authError=google_fallo");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void cancelarEnGoogleVuelveALaClinicaConUnAviso() {
        String state = empezarLogin();

        var redireccion = controller.googleCallback(null, state, "access_denied", state, new MockHttpServletResponse());

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/vargas-vet/login?authError=google_cancelado");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void unCorreoNoVerificadoPorGoogleNoEntra() {
        String state = empezarLogin();
        when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", false));

        var redireccion = volverDeGoogle("codigo", state, state);

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/vargas-vet/login?authError=google_email_no_verificado");
        verifyNoInteractions(usuarioService);
    }

    @Test
    void cadaMotivoDeRechazoLlevaASuMensaje() {
        record Caso(RuntimeException error, String codigo) {}
        var casos = java.util.List.of(
                new Caso(new GoogleClinicAccessException(), "google_sin_acceso_clinica"),
                new Caso(new GoogleAccountSuspendedException(), "google_cuenta_suspendida"),
                new Caso(new veterinaria.vargasvet.exception.GoogleAccountClosedException(), "google_cuenta_cerrada"),
                new Caso(new veterinaria.vargasvet.exception.GoogleAccountDeactivatedException(), "google_cuenta_dada_de_baja"),
                new Caso(new DisabledException("sin rol"), "google_cuenta_no_habilitada"),
                new Caso(new GoogleEmailMismatchException(), "google_fallo"),
                new Caso(new IllegalStateException("inesperado"), "google_fallo"));
        for (Caso caso : casos) {
            String state = empezarLogin();
            when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                    .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", true));
            doThrow(caso.error()).when(usuarioService).loginWithGoogle(anyString(), anyString());

            var redireccion = volverDeGoogle("codigo", state, state);

            assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/vargas-vet/login?authError=" + caso.codigo());
        }
    }

    @Test
    @DisplayName("La activación lleva el token solo en el servidor: nunca aparece en lo que se envía a Google")
    void laActivacionNoLlevaElTokenPorGoogle() {
        iniciar(intencion(null, "token-de-invitacion"));

        assertThat(estadoEnviadoAGoogle).doesNotContain("token-de-invitacion");
        assertThat(desafioEnviadoAGoogle).doesNotContain("token-de-invitacion");
    }

    @Test
    void laActivacionConGoogleUsaElTokenGuardadoYNoHaceLoginNormal() {
        iniciar(intencion(null, "token-de-invitacion"));
        String state = estadoEnviadoAGoogle;
        when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("ana@example.test", true));
        AuthResponse sesion = new AuthResponse();
        when(usuarioService.activateAccountWithGoogle("token-de-invitacion", "ana@example.test")).thenReturn(sesion);
        when(exchangeStore.store(sesion)).thenReturn("canje-4");

        var redireccion = volverDeGoogle("codigo", state, state);

        assertThat(destino(redireccion)).isEqualTo(FRONTEND + "/auth/google/callback?code=canje-4");
        verify(usuarioService, never()).loginWithGoogle(anyString(), anyString());
    }

    @Test
    void siElCorreoNoCoincideConLaInvitacionSeVuelveALaPantallaDeActivacion() {
        iniciar(intencion(null, "token-de-invitacion"));
        String state = estadoEnviadoAGoogle;
        when(googleOAuthService.resolveIdentity(anyString(), anyString()))
                .thenReturn(new GoogleOAuthService.GoogleIdentity("otra@example.test", true));
        when(usuarioService.activateAccountWithGoogle(anyString(), anyString())).thenThrow(new GoogleEmailMismatchException());

        var redireccion = volverDeGoogle("codigo", state, state);

        assertThat(destino(redireccion)).isEqualTo(
                FRONTEND + "/auth/verify?authError=google_correo_no_coincide#token=token-de-invitacion");
    }

    @Test
    void elCanjeEntregaLaSesionConCookiesYElCodigoVencidoSeRechaza() {
        AuthResponse sesion = new AuthResponse();
        sesion.setToken("acceso");
        sesion.setRefreshToken("refresco");
        when(exchangeStore.consume("bueno")).thenReturn(sesion);
        GoogleExchangeRequest bueno = new GoogleExchangeRequest();
        bueno.setCode("bueno");
        GoogleExchangeRequest vencido = new GoogleExchangeRequest();
        vencido.setCode("vencido");
        MockHttpServletResponse respuestaHttp = new MockHttpServletResponse();

        var ok = controller.googleExchange(bueno, respuestaHttp);
        var rechazado = controller.googleExchange(vencido, new MockHttpServletResponse());

        assertThat(ok.getStatusCode().value()).isEqualTo(200);
        assertThat(respuestaHttp.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("HttpOnly").contains("Secure"));
        assertThat(rechazado.getStatusCode().value()).isEqualTo(401);
    }
}
