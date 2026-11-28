package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.time.Duration;

/** El callback de Google (GET, navegación del navegador) no puede devolverle los tokens al frontend por URL sin
 * exponerlos en el historial o en los registros: guarda aquí la respuesta ya calculada bajo un código de un solo
 * uso de corta duración y el frontend lo canjea con un POST. El estado vive en PostgreSQL, cifrado, para que el
 * canje funcione en cualquier instancia del backend. */
@Component
@RequiredArgsConstructor
public class GoogleLoginExchangeStore {

    private static final String SESION = "SESION";
    private static final String REACTIVACION = "REACTIVACION";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Duration REACTIVATION_TTL = Duration.ofMinutes(5);

    public record Reactivation(Integer usuarioId, Integer companyId, String email, String slug) {}

    public record SesionGuardada(AuthResponse respuesta, String token, String refreshToken) {}

    private final EstadoOAuthCompartido estado;

    public String store(AuthResponse response) {
        return estado.guardar(SESION, new SesionGuardada(response, response.getToken(), response.getRefreshToken()), TTL, 0);
    }

    public AuthResponse consume(String code) {
        SesionGuardada guardada = estado.consumir(SESION, code, SesionGuardada.class);
        if (guardada == null || guardada.respuesta() == null) {
            return null;
        }
        AuthResponse respuesta = guardada.respuesta();
        respuesta.setToken(guardada.token());
        respuesta.setRefreshToken(guardada.refreshToken());
        return respuesta;
    }

    public String storeReactivation(Reactivation data) {
        return estado.guardar(REACTIVACION, data, REACTIVATION_TTL, 0);
    }

    public Reactivation consumeReactivation(String ticket) {
        return estado.consumir(REACTIVACION, ticket, Reactivation.class);
    }
}
