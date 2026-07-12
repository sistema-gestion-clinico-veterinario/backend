package veterinaria.vargasvet.e2e;

import lombok.Getter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Component
@Profile("e2e")
public class E2eFixtureRegistry {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Map<String, Object> snapshot() {
        return Map.copyOf(values);
    }
}
