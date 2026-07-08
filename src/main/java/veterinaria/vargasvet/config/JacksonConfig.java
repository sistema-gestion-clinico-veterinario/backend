package veterinaria.vargasvet.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule stringTrimModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new StdScalarDeserializer<String>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                if (value == null) return null;
                String fieldName = p.currentName();
                if (fieldName != null && fieldName.toLowerCase().contains("password")) {
                    return value;
                }
                return value
                        .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                        .replaceAll("(?is)<\\s*script\\b[^>]*>.*?<\\s*/\\s*script\\s*>", "")
                        .trim()
                        .replaceAll("[ \t\\x0B\f\r]+", " ");
            }
        });
        return module;
    }
}
