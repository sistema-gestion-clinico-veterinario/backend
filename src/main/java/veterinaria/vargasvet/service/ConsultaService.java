package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;

public interface ConsultaService {
    ConsultaResponse updateConsulta(Long id, ConsultaRequest request);
    ConsultaResponse getConsultaById(Long id);
    ConsultaResponse cerrarConsulta(Long id, CerrarConsultaRequest request);
}
