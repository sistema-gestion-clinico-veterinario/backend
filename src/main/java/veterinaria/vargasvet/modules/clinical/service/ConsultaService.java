package veterinaria.vargasvet.modules.clinical.service;

import veterinaria.vargasvet.modules.clinical.dto.CerrarConsultaRequest;
import veterinaria.vargasvet.modules.clinical.dto.ConsultaRequest;
import veterinaria.vargasvet.modules.clinical.dto.ConsultaResponse;

public interface ConsultaService {
    ConsultaResponse updateConsulta(Long id, ConsultaRequest request);
    ConsultaResponse getConsultaById(Long id);
    ConsultaResponse cerrarConsulta(Long id, CerrarConsultaRequest request);
}
