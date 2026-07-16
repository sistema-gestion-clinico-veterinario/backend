package veterinaria.vargasvet.selenium.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Fixtures {
    private final JsonObject values;

    Fixtures(JsonObject values) {
        this.values = values;
    }

    public String text(String key) {
        JsonElement value = required(key);
        return value.getAsString();
    }

    public long number(String key) {
        JsonElement value = required(key);
        return value.getAsLong();
    }

    public boolean bool(String key) {
        JsonElement value = required(key);
        return value.getAsBoolean();
    }

    public boolean has(String key) {
        return values.has(key) && !values.get(key).isJsonNull();
    }

    private JsonElement required(String key) {
        if (!has(key)) {
            throw new IllegalStateException("El fixture E2E no contiene la clave requerida: " + key);
        }
        return values.get(key);
    }
}
