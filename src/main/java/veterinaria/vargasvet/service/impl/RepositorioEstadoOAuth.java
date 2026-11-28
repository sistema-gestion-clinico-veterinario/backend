package veterinaria.vargasvet.service.impl;

import java.util.Optional;

public interface RepositorioEstadoOAuth {

    record Retirado(String contenido, boolean vigente) {}

    void purgarVencidos();

    int contar(String tipo);

    void insertar(String tipo, String claveHash, String contenido, long vidaSegundos);

    Optional<Retirado> retirar(String tipo, String claveHash);
}
