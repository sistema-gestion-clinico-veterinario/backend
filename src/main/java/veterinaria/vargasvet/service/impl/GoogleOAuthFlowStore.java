package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Estado de un inicio de sesión (o activación) con Google. Vive en PostgreSQL, cifrado, para que cualquier
 * instancia del backend pueda atender cualquier paso del flujo; ni el slug ni el token de invitación viajan por
 * la URL que pasa por Google. */
@Component
@RequiredArgsConstructor
public class GoogleOAuthFlowStore {

    private static final String INTENT = "INTENT";
    private static final String AUTORIZACION = "AUTORIZACION";
    private static final Duration INTENT_TTL = Duration.ofMinutes(5);
    private static final Duration AUTHORIZATION_TTL = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 10_000;

    public record Context(String slug, String activationToken) {
        @JsonIgnore
        public boolean isActivation() {
            return activationToken != null;
        }
    }

    public record Authorization(Context context, String codeVerifier) {}

    private final EstadoOAuthCompartido estado;

    public String createIntent(Context context) {
        return estado.guardar(INTENT, context, INTENT_TTL, MAX_ENTRIES);
    }

    public Context consumeIntent(String intent) {
        return estado.consumir(INTENT, intent, Context.class);
    }

    public String createAuthorization(Authorization authorization) {
        return estado.guardar(AUTORIZACION, authorization, AUTHORIZATION_TTL, MAX_ENTRIES);
    }

    public Authorization consumeAuthorization(String state) {
        return estado.consumir(AUTORIZACION, state, Authorization.class);
    }
}
