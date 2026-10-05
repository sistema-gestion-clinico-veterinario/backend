package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/** Intercambia el "code" de autorización que Google le entrega al navegador (tras el
 * consentimiento) por el email VERIFICADO de la persona - llamada servidor a servidor,
 * el Client Secret nunca sale del backend. */
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestTemplate restTemplate;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    public record GoogleIdentity(String email, boolean emailVerified) {}

    public String buildAuthorizationUrl(String state, String codeChallenge) {
        if (clientId.isBlank()) {
            throw new IllegalStateException("Google OAuth no está configurado (GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET)");
        }
        return AUTHORIZATION_ENDPOINT
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&prompt=select_account"
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    public static String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public GoogleIdentity resolveIdentity(String authorizationCode, String codeVerifier) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Google OAuth no está configurado (GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET)");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", authorizationCode);
        body.add("code_verifier", codeVerifier);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String accessToken;
        try {
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    TOKEN_ENDPOINT, new HttpEntity<>(body, headers), Map.class);
            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                throw new BadCredentialsException("No se pudo validar la sesión de Google");
            }
            accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                throw new BadCredentialsException("No se pudo validar la sesión de Google");
            }
        } catch (RestClientException ex) {
            throw new BadCredentialsException("No se pudo validar la sesión de Google");
        }

        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);
        try {
            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    USERINFO_ENDPOINT, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(userInfoHeaders), Map.class);
            if (userInfoResponse.getStatusCode() != HttpStatus.OK || userInfoResponse.getBody() == null) {
                throw new BadCredentialsException("No se pudo validar la sesión de Google");
            }
            Map<?, ?> payload = userInfoResponse.getBody();
            Object email = payload.get("email");
            Object emailVerified = payload.get("email_verified");
            if (!(email instanceof String)) {
                throw new BadCredentialsException("No se pudo validar la sesión de Google");
            }
            boolean verified = Boolean.TRUE.equals(emailVerified) || "true".equals(String.valueOf(emailVerified));
            return new GoogleIdentity((String) email, verified);
        } catch (RestClientException ex) {
            throw new BadCredentialsException("No se pudo validar la sesión de Google");
        }
    }
}
