package veterinaria.vargasvet.clinica;

import veterinaria.vargasvet.clinica.CerrarConsultaRequest;
import veterinaria.vargasvet.clinica.ConsultaRequest;
import veterinaria.vargasvet.clinica.ConsultaResponse;

public interface ConsultaService {
    ConsultaResponse updateConsulta(Long id, ConsultaRequest request);
    ConsultaResponse getConsultaById(Long id);
    ConsultaResponse cerrarConsulta(Long id, CerrarConsultaRequest request);
}
