package veterinaria.vargasvet.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RepositorioEstadoOAuthEnMemoria implements RepositorioEstadoOAuth {

    private record Fila(String tipo, String contenido, Instant expiraEn) {}

    private final Map<String, Fila> filas = new ConcurrentHashMap<>();
    private volatile Supplier<Instant> reloj = Instant::now;

    void fijarReloj(Instant momento) {
        this.reloj = () -> momento;
    }

    int tamano() {
        return filas.size();
    }

    String volcado() {
        StringBuilder texto = new StringBuilder();
        filas.forEach((clave, fila) -> texto.append(clave).append('|').append(fila.contenido()).append('\n'));
        return texto.toString();
    }

    @Override
    public void purgarVencidos() {
        Instant ahora = reloj.get();
        filas.values().removeIf(fila -> fila.expiraEn().isBefore(ahora));
    }

    @Override
    public int contar(String tipo) {
        return (int) filas.values().stream().filter(fila -> fila.tipo().equals(tipo)).count();
    }

    @Override
    public void insertar(String tipo, String claveHash, String contenido, long vidaSegundos) {
        filas.put(tipo + ':' + claveHash, new Fila(tipo, contenido, reloj.get().plus(Duration.ofSeconds(vidaSegundos))));
    }

    @Override
    public Optional<Retirado> retirar(String tipo, String claveHash) {
        Fila fila = filas.remove(tipo + ':' + claveHash);
        if (fila == null) {
            return Optional.empty();
        }
        return Optional.of(new Retirado(fila.contenido(), fila.expiraEn().isAfter(reloj.get())));
    }
}
