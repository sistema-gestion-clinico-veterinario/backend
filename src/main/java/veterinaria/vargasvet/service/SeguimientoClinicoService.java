package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.response.SugerenciaControlResponse;

import java.util.List;

public interface SeguimientoClinicoService {
    List<SugerenciaControlResponse> listarSugerenciasControl(Integer companyId, int diasVentana);
}
