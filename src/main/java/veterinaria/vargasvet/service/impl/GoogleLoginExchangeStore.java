package veterinaria.vargasvet.service.impl;

import org.springframework.stereotype.Component;
import veterinaria.vargasvet.dto.response.AuthResponse;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** El callback de Google (GET, navegación del navegador) no puede devolverle los tokens
 * directamente al frontend por URL sin exponerlos en el historial/logs - en su lugar, el
 * callback guarda aquí la respuesta ya calculada bajo un código de un solo uso de corta
 * duración y redirige al frontend con ese código; el frontend lo canjea con un POST
 * (mismo patrón que un "authorization code" de un solo uso). En memoria: alcanza porque
 * el código vive segundos y el backend corre en una sola instancia. */
@Component
public class GoogleLoginExchangeStore {

    private static final java.time.Duration TTL = java.time.Duration.ofSeconds(60);

    private record Entry(AuthResponse response, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String store(AuthResponse response) {
        // Purga oportunista: sin @EnableScheduling en la app, este es el único
        // momento en que el mapa se limpia de códigos ya vencidos.
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));

        String code = UUID.randomUUID().toString();
        store.put(code, new Entry(response, now.plus(TTL)));
        return code;
    }

    /** Canje de un solo uso: se retira del mapa aunque haya expirado. */
    public AuthResponse consume(String code) {
        Entry entry = store.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.response();
    }
}
