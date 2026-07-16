package veterinaria.vargasvet.selenium.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class FixtureClient {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new Gson();

    public Fixtures load() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(E2eConfig.API_URL + "/setup/e2e/fixtures"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("No se pudieron obtener fixtures E2E: HTTP " + response.statusCode());
            }
            return new Fixtures(gson.fromJson(response.body(), JsonObject.class));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo conectar con el backend E2E", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la lectura de fixtures E2E", e);
        }
    }

    public void post(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(E2eConfig.API_URL + path))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Falló el soporte E2E " + path + ": HTTP "
                        + response.statusCode() + " - " + response.body());
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo invocar el soporte E2E " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió el soporte E2E " + path, e);
        }
    }
}
