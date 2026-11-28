package veterinaria.vargasvet.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class EstadoOAuthParaPruebas {

    private EstadoOAuthParaPruebas() {
    }

    public static EstadoOAuthCompartido enMemoria() {
        return new EstadoOAuthCompartido(new RepositorioEstadoOAuthEnMemoria(), new CifradoEstadoOAuth(new byte[32]), new ObjectMapper());
    }
}
