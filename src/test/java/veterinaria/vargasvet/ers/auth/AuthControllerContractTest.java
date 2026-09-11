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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerContractTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final EmailChangeService emailChangeService = mock(EmailChangeService.class);
    private final AuthController controller = new AuthController(usuarioService, emailChangeService);

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
        request.setEmail("persona@example.com");
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
    @DisplayName("[CP-RF03-01] El cambio de contraseña se aplica a la identidad autenticada")
    void cambioDePasswordUsaIdentidadAutenticada() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("actual@example.com", "token"));
        ChangePasswordDTO request = new ChangePasswordDTO();

        controller.changePassword(request);

        verify(usuarioService).changePassword("actual@example.com", request);
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
