package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CartillaAplicacionRequest;
import veterinaria.vargasvet.dto.response.CartillaAplicacionResponse;

public interface CartillaService {

    CartillaAplicacionResponse registrarVacunacion(CartillaAplicacionRequest request);

    CartillaAplicacionResponse registrarDesparasitacion(CartillaAplicacionRequest request);

}
