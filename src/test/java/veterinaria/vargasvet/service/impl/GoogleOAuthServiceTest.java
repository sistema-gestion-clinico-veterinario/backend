package veterinaria.vargasvet.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class GoogleOAuthServiceTest {

    private static final String TOKEN = "https://oauth2.googleapis.com/token";
    private static final String USERINFO = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Mock RestTemplate restTemplate;

    private GoogleOAuthService service;

    @BeforeEach
    void setUp() {
        service = new GoogleOAuthService(restTemplate);
        ReflectionTestUtils.setField(service, "clientId", "cliente-123.apps.googleusercontent.com");
        ReflectionTestUtils.setField(service, "clientSecret", "secreto");
        ReflectionTestUtils.setField(service, "redirectUri", "https://backend.test/api/v1/auth/google/callback");
    }

    private void googleResponde(Map<String, Object> userInfo) {
        when(restTemplate.postForEntity(eq(TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("access_token", "token-de-acceso")));
        when(restTemplate.exchange(eq(USERINFO), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(userInfo));
    }

    @Test
    void elDesafioPkceCoincideConElEjemploDeLaRfc7636() {
        assertThat(GoogleOAuthService.codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
                .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }

    @Test
    void laUrlDeAutorizacionLlevaStatePkceYLosParametrosCodificados() {
        String url = service.buildAuthorizationUrl("estado-aleatorio", "desafio-pkce");

        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                .contains("client_id=cliente-123.apps.googleusercontent.com")
                .contains("redirect_uri=https%3A%2F%2Fbackend.test%2Fapi%2Fv1%2Fauth%2Fgoogle%2Fcallback")
                .contains("response_type=code")
                .contains("scope=openid+email+profile")
                .contains("prompt=select_account")
                .contains("state=estado-aleatorio")
                .contains("code_challenge=desafio-pkce")
                .contains("code_challenge_method=S256")
                .doesNotContain("secreto");
    }

    @Test
    void sinClienteConfiguradoNoArmaLaUrl() {
        ReflectionTestUtils.setField(service, "clientId", "");

        assertThatThrownBy(() -> service.buildAuthorizationUrl("s", "c"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void elCodigoSeCanjeaConElVerificadorPkceYElSecretoSoloViajaDeServidorAServidor() {
        googleResponde(Map.of("email", "ana@example.test", "email_verified", true));

        GoogleOAuthService.GoogleIdentity identidad = service.resolveIdentity("codigo-de-google", "verificador-pkce");

        assertThat(identidad.email()).isEqualTo("ana@example.test");
        assertThat(identidad.emailVerified()).isTrue();
        ArgumentCaptor<HttpEntity> pedido = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(TOKEN), pedido.capture(), eq(Map.class));
        MultiValueMap<String, String> cuerpo = (MultiValueMap<String, String>) pedido.getValue().getBody();
        assertThat(cuerpo.getFirst("code")).isEqualTo("codigo-de-google");
        assertThat(cuerpo.getFirst("code_verifier")).isEqualTo("verificador-pkce");
        assertThat(cuerpo.getFirst("grant_type")).isEqualTo("authorization_code");
        assertThat(cuerpo.getFirst("client_secret")).isEqualTo("secreto");
    }

    @Test
    void elCorreoSinVerificarSeReportaComoNoVerificado() {
        googleResponde(Map.of("email", "ana@example.test", "email_verified", false));

        assertThat(service.resolveIdentity("c", "v").emailVerified()).isFalse();
    }

    @Test
    void laBanderaDeVerificacionTambienSeAceptaComoTexto() {
        googleResponde(Map.of("email", "ana@example.test", "email_verified", "true"));

        assertThat(service.resolveIdentity("c", "v").emailVerified()).isTrue();
    }

    @Test
    void sinBanderaDeVerificacionNoSeConsideraVerificado() {
        googleResponde(Map.of("email", "ana@example.test"));

        assertThat(service.resolveIdentity("c", "v").emailVerified()).isFalse();
    }

    @Test
    void siGoogleRechazaElCodigoSeRechazaElAcceso() {
        when(restTemplate.postForEntity(eq(TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("invalid_grant"));

        assertThatThrownBy(() -> service.resolveIdentity("c", "v"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("No se pudo validar la sesión de Google");
    }

    @Test
    void sinTokenDeAccesoEnLaRespuestaSeRechazaElAcceso() {
        when(restTemplate.postForEntity(eq(TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("otro", "dato")));

        assertThatThrownBy(() -> service.resolveIdentity("c", "v")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void siLaRespuestaDeTokenNoEsExitosaSeRechazaElAcceso() {
        when(restTemplate.postForEntity(eq(TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid_grant")));

        assertThatThrownBy(() -> service.resolveIdentity("c", "v")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void sinCorreoEnLosDatosDeLaPersonaSeRechazaElAcceso() {
        googleResponde(Map.of("sub", "123"));

        assertThatThrownBy(() -> service.resolveIdentity("c", "v")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void sinSecretoConfiguradoNoSeIntentaNada() {
        ReflectionTestUtils.setField(service, "clientSecret", "");

        assertThatThrownBy(() -> service.resolveIdentity("c", "v")).isInstanceOf(IllegalStateException.class);
    }
}
