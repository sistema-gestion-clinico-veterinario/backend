package veterinaria.vargasvet.ers.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.controller.AuthController;
import veterinaria.vargasvet.dto.request.AccountClosureConfirmRequest;
import veterinaria.vargasvet.dto.request.AccountClosureRequest;
import veterinaria.vargasvet.dto.request.ReactivateAccountRequest;
import veterinaria.vargasvet.dto.response.AccountClosureEligibility;
import veterinaria.vargasvet.service.AccountClosureService;
import veterinaria.vargasvet.service.EmailChangeService;
import veterinaria.vargasvet.service.UsuarioService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerAccountClosureTest {

    private final AccountClosureService service = mock(AccountClosureService.class);
    private final AuthController controller = new AuthController(
            mock(UsuarioService.class), mock(EmailChangeService.class), mock(GoogleOAuthService.class),
            mock(GoogleLoginExchangeStore.class), new GoogleOAuthFlowStore(), service,
                org.mockito.Mockito.mock(veterinaria.vargasvet.security.ClientIpResolver.class));

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
    }

    @Test
    void laConsultaDevuelveSiPuedeCerrarYPorQueNo() {
        when(service.eligibility()).thenReturn(new AccountClosureEligibility(false, "Tienes la caja abierta", true));

        var respuesta = controller.closureEligibility();

        assertThat(respuesta.getBody().getData().eligible()).isFalse();
        assertThat(respuesta.getBody().getData().reason()).isEqualTo("Tienes la caja abierta");
        assertThat(respuesta.getBody().getData().requiresPassword()).isTrue();
    }

    @Test
    void pedirElCodigoLlevaLaContrasenaYAvisaQueSeEnvioAlCorreo() {
        AccountClosureRequest request = new AccountClosureRequest();
        request.setPassword("Clave-123");

        var respuesta = controller.requestAccountClosure(request);

        verify(service).requestClosure("Clave-123");
        assertThat(respuesta.getBody().getMessage()).contains("código de 6 dígitos").contains("10 minutos");
    }

    @Test
    void pedirElCodigoDevuelveCuantoHayQueEsperarParaPedirOtro() {
        when(service.requestClosure("Clave-123")).thenReturn(120L);
        AccountClosureRequest request = new AccountClosureRequest();
        request.setPassword("Clave-123");

        var respuesta = controller.requestAccountClosure(request);

        assertThat(respuesta.getBody().getData()).containsEntry("retryAfterSeconds", 120L);
    }

    @Test
    void confirmarElCierreCierraLaSesionDelNavegadorYExplicaLosPasosSiguientes() {
        AccountClosureConfirmRequest request = new AccountClosureConfirmRequest();
        request.setCode("482913");
        MockHttpServletResponse http = new MockHttpServletResponse();

        var respuesta = controller.confirmAccountClosure(request, new org.springframework.mock.web.MockHttpServletRequest(), http);

        verify(service).confirmClosure("482913");
        assertThat(http.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0").contains("HttpOnly"));
        assertThat(respuesta.getBody().getMessage()).contains("Cerraste tu cuenta").contains("30 días");
    }

    @Test
    void alCerrarLaCuentaSoloSeBorranLasCookiesDeEsaClinica() {
        AccountClosureConfirmRequest request = new AccountClosureConfirmRequest();
        request.setCode("482913");
        org.springframework.mock.web.MockHttpServletRequest http = new org.springframework.mock.web.MockHttpServletRequest();
        http.addHeader(veterinaria.vargasvet.security.SessionCookies.SLUG_HEADER, "clinica-a");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        controller.confirmAccountClosure(request, http, respuesta);

        assertThat(respuesta.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
                .anySatisfy(cookie -> assertThat(cookie).startsWith("access_token__clinica-a=;"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("refresh_token__clinica-a=;"));
    }

    @Test
    void reactivarUsaElEnlaceDelCorreoYNoNecesitaSesion() {
        ReactivateAccountRequest request = new ReactivateAccountRequest();
        request.setToken("token-del-correo");

        var respuesta = controller.reactivateAccount(request);

        verify(service).reactivate("token-del-correo");
        assertThat(respuesta.getBody().getMessage()).contains("Ya puedes iniciar sesión");
    }
}
