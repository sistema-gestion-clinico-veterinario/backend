package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.VentaLibreRequest;
import veterinaria.vargasvet.dto.response.VentaLibreResponse;

public interface VentaLibreService {
    VentaLibreResponse registrar(VentaLibreRequest request);
}
