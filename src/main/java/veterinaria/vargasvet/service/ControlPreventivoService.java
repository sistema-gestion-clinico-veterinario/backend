package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;

import java.util.List;

public interface ControlPreventivoService {
    List<TipoVacunaResponse> listarTiposVacuna(Long mascotaId);
    TipoVacunaResponse crearTipoVacuna(TipoVacunaRequest request);
    List<ControlPreventivoResponse> listarControles(Long mascotaId);
    List<AplicacionPreventivaResponse> listarAplicaciones(Long mascotaId);
    ControlPreventivoResponse programar(Long mascotaId, ControlPreventivoRequest request);
    ControlPreventivoResponse registrarVacunacion(Long consultaId, RegistroVacunacionRequest request);
    ControlPreventivoResponse registrarDesparasitacion(Long consultaId, RegistroDesparasitacionRequest request);
}
