package veterinaria.vargasvet.ers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import veterinaria.vargasvet.controller.AuthController;
import veterinaria.vargasvet.dto.request.ChangePasswordDTO;
import veterinaria.vargasvet.dto.request.ConfirmSecurityTokenDTO;
import veterinaria.vargasvet.dto.request.ForgotPasswordRequest;
import veterinaria.vargasvet.dto.request.LoginDTO;
import veterinaria.vargasvet.dto.response.AuthResponse;
import veterinaria.vargasvet.service.EmailChangeService;
import veterinaria.vargasvet.service.UsuarioService;
import veterinaria.vargasvet.service.impl.GoogleLoginExchangeStore;
import veterinaria.vargasvet.service.impl.GoogleOAuthService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerContractTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final EmailChangeService emailChangeService = mock(EmailChangeService.class);
    private final GoogleOAuthService googleOAuthService = mock(GoogleOAuthService.class);
    private final GoogleLoginExchangeStore googleLoginExchangeStore = mock(GoogleLoginExchangeStore.class);
    private final veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore googleOAuthFlowStore = mock(veterinaria.vargasvet.service.impl.GoogleOAuthFlowStore.class);
    private final veterinaria.vargasvet.service.AccountClosureService accountClosureService = mock(veterinaria.vargasvet.service.AccountClosureService.class);
    private final AuthController controller = new AuthController(
            usuarioService, emailChangeService, googleOAuthService, googleLoginExchangeStore, googleOAuthFlowStore, accountClosureService,
                org.mockito.Mockito.mock(veterinaria.vargasvet.security.ClientIpResolver.class));

    @BeforeEach
    void configureCookies() {
        ReflectionTestUtils.setField(controller, "cookieSecure", true);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(controller, "accessTokenMaxAge", 1800L);
        ReflectionTestUtils.setField(controller, "refreshTokenMaxAge", 604800L);
        ReflectionTestUtils.invokeMethod(controller, "validateCookieConfiguration");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("[CP-RF01-01][CP-RNF03-01] El login entrega tokens solo mediante cookies protegidas")
    void loginNoExponeTokensEnJson() throws Exception {
        LoginDTO request = new LoginDTO();
        request.setSlug("clinica-vargasvet");
        request.setUsername("persona");
        request.setPassword("Frase extensa de prueba 2026");

        AuthResponse auth = new AuthResponse();
        auth.setToken("access-secret");
        auth.setRefreshToken("refresh-secret");
        auth.setRoles(List.of("VETERINARIO"));
        when(usuarioService.login(request)).thenReturn(auth);

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        var response = controller.login(request, servletResponse);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(servletResponse.getHeaders(HttpHeaders.SET_COOKIE)).hasSize(2)
                .allSatisfy(cookie -> assertThat(cookie)
                        .contains("HttpOnly")
                        .contains("Secure")
                        .contains("SameSite=Lax"));
        assertThat(new ObjectMapper().writeValueAsString(response.getBody()))
                .doesNotContain("access-secret", "refresh-secret", "\"token\"", "\"refreshToken\"");
    }

    @Test
    @DisplayName("[CP-RF02-01] La solicitud de recuperación usa una respuesta neutra")
    void recuperacionNoRevelaSiExisteElCorreo() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("desconocido@example.com");

        var response = controller.forgotPassword(request);

        verify(usuarioService).forgotPassword(request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Si el correo existe");
    }

    @Test
    @DisplayName("[CP-RF02-02] La recuperación responde con un tiempo mínimo para no delatar qué correos tienen cuenta")
    void recuperacionRespondeConTiempoMinimo() {
        ReflectionTestUtils.setField(controller, "recoveryMinResponseMs", 150L);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("desconocido@example.com");

        long inicio = System.nanoTime();
        controller.forgotPassword(request);
        long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000L;

        assertThat(transcurridoMs).isGreaterThanOrEqualTo(140L);
    }

    @Test
    @DisplayName("[CP-RF03-01] El cambio de contraseña se aplica a la identidad autenticada")
    void cambioDePasswordUsaIdentidadAutenticada() {
        veterinaria.vargasvet.security.UsuarioPrincipal principal = new veterinaria.vargasvet.security.UsuarioPrincipal(
                7, "actual@example.com", "hash", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token"));
        ChangePasswordDTO request = new ChangePasswordDTO();

        controller.changePassword(request);

        verify(usuarioService).changePassword(7, request);
    }

    @Test
    @DisplayName("[CP-RF-AUT-08-01] El estado de la sesión se consulta con la identidad autenticada y no emite cookies")
    void estadoDeSesionUsaIdentidadAutenticadaYNoEmiteCookies() throws Exception {
        veterinaria.vargasvet.security.UsuarioPrincipal principal = new veterinaria.vargasvet.security.UsuarioPrincipal(
                7, "actual@example.com", "hash", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token"));
        AuthResponse auth = new AuthResponse();
        auth.setToken("access-secret");
        auth.setRoles(List.of("VETERINARIO"));
        when(usuarioService.currentSession(7)).thenReturn(auth);

        var response = controller.currentSession();

        verify(usuarioService).currentSession(7);
        assertThat(new ObjectMapper().writeValueAsString(response.getBody())).doesNotContain("access-secret");
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("[CP-RF-AUT-02-01] No existe un alta de usuarios con contraseña enviada por quien llama")
    void noExisteEndpointDeRegistroConContrasenaAjena() {
        var rutas = java.util.Arrays.stream(AuthController.class.getDeclaredMethods())
                .flatMap(method -> java.util.stream.Stream.of(method.getAnnotations()))
                .filter(annotation -> annotation instanceof org.springframework.web.bind.annotation.PostMapping)
                .flatMap(annotation -> java.util.Arrays.stream(
                        ((org.springframework.web.bind.annotation.PostMapping) annotation).value()))
                .toList();

        assertThat(rutas).doesNotContain("/register");
        assertThat(java.util.Arrays.stream(UsuarioService.class.getDeclaredMethods()))
                .noneMatch(method -> method.getName().equals("register"));
    }

    @Test
    @DisplayName("[CP-RF-AUT-09-01] La suspensión se hace solo por la gestión de empleados y clientes, no por un endpoint aparte")
    void noExisteEndpointDeSuspensionAparte() {
        var rutas = java.util.Arrays.stream(AuthController.class.getDeclaredMethods())
                .flatMap(method -> java.util.stream.Stream.of(method.getAnnotations()))
                .filter(annotation -> annotation instanceof org.springframework.web.bind.annotation.PutMapping)
                .flatMap(annotation -> java.util.Arrays.stream(
                        ((org.springframework.web.bind.annotation.PutMapping) annotation).value()))
                .toList();

        assertThat(rutas).doesNotContain("/suspend/{id}");
        assertThat(java.util.Arrays.stream(UsuarioService.class.getDeclaredMethods()))
                .noneMatch(method -> method.getName().equals("suspendAccount"));
    }

    @Test
    @DisplayName("[CP-RF04-02] Las confirmaciones de correo permanecen separadas")
    void cambioCorreoMantieneDosConfirmaciones() {
        ConfirmSecurityTokenDTO current = new ConfirmSecurityTokenDTO();
        current.setToken("current-token");
        ConfirmSecurityTokenDTO replacement = new ConfirmSecurityTokenDTO();
        replacement.setToken("new-token");
        when(emailChangeService.confirmCurrentEmail("current-token")).thenReturn(false);
        when(emailChangeService.confirmNewEmail("new-token")).thenReturn(true);

        var first = controller.confirmCurrentEmail(current);
        var second = controller.confirmNewEmail(replacement);

        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().getData()).isFalse();
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().getData()).isTrue();
    }
}
