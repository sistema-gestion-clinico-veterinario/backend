package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.RazaRequest;
import veterinaria.vargasvet.dto.response.RazaResponse;

import java.util.List;

public interface RazaService {
    List<RazaResponse> listarPorEspecie(String especie, Long companyId);
    RazaResponse crear(RazaRequest request, Long companyId);
}
