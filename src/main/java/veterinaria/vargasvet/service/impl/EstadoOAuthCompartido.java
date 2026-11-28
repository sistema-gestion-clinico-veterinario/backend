package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import veterinaria.vargasvet.exception.RateLimitExceededException;
import veterinaria.vargasvet.security.SecurityTokenUtils;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class EstadoOAuthCompartido {

    private final RepositorioEstadoOAuth repositorio;
    private final CifradoEstadoOAuth cifrado;
    private final ObjectMapper mapper;

    public EstadoOAuthCompartido(RepositorioEstadoOAuth repositorio, CifradoEstadoOAuth cifrado, ObjectMapper mapper) {
        this.repositorio = repositorio;
        this.cifrado = cifrado;
        this.mapper = mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String guardar(String tipo, Object valor, Duration vida, int maximo) {
        repositorio.purgarVencidos();
        if (maximo > 0 && repositorio.contar(tipo) >= maximo) {
            throw new RateLimitExceededException(vida);
        }
        String clave = SecurityTokenUtils.generate();
        String hash = SecurityTokenUtils.hash(clave);
        repositorio.insertar(tipo, hash, cifrado.cifrar(serializar(valor), contexto(tipo, hash)), vida.toSeconds());
        return clave;
    }

    public <T> T consumir(String tipo, String clave, Class<T> clase) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        String hash = SecurityTokenUtils.hash(clave);
        Optional<RepositorioEstadoOAuth.Retirado> fila = repositorio.retirar(tipo, hash);
        if (fila.isEmpty() || !fila.get().vigente()) {
            return null;
        }
        try {
            return mapper.readValue(cifrado.descifrar(fila.get().contenido(), contexto(tipo, hash)), clase);
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            log.warn("Estado de OAuth ({}) ilegible: {}", tipo, ex.getClass().getSimpleName());
            return null;
        }
    }

    private String serializar(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el estado de OAuth", ex);
        }
    }

    private static String contexto(String tipo, String hash) {
        return tipo + ':' + hash;
    }
}
