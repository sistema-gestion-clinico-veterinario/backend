package veterinaria.vargasvet.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.dto.request.*;
import veterinaria.vargasvet.dto.response.AplicacionPreventivaResponse;
import veterinaria.vargasvet.dto.response.ControlPreventivoResponse;
import veterinaria.vargasvet.dto.response.TipoDesparasitanteResponse;
import veterinaria.vargasvet.dto.response.TipoVacunaResponse;

import java.util.List;

public interface ControlPreventivoService {
    List<TipoVacunaResponse> listarTiposVacuna(Long mascotaId);
    TipoVacunaResponse crearTipoVacuna(TipoVacunaRequest request);
    List<TipoDesparasitanteResponse> listarTiposDesparasitante(Long mascotaId);
    TipoDesparasitanteResponse crearTipoDesparasitante(TipoDesparasitanteRequest request);
    List<ControlPreventivoResponse> listarControles(Long mascotaId);
    List<AplicacionPreventivaResponse> listarAplicaciones(Long mascotaId);
    ControlPreventivoResponse programar(Long mascotaId, ControlPreventivoRequest request);
    ControlPreventivoResponse reprogramar(Long controlId, ReprogramarControlPreventivoRequest request);
    ControlPreventivoResponse cancelar(Long controlId);
    ControlPreventivoResponse registrarVacunacion(Long consultaId, RegistroVacunacionRequest request);
    ControlPreventivoResponse registrarDesparasitacion(Long consultaId, RegistroDesparasitacionRequest request);

    Page<TipoVacunaResponse> listarTiposVacunaPorCompany(Integer companyId, Pageable pageable);
    Page<TipoDesparasitanteResponse> listarTiposDesparasitantePorCompany(Integer companyId, Pageable pageable);
    TipoVacunaResponse actualizarTipoVacuna(Long id, TipoVacunaRequest request);
    TipoDesparasitanteResponse actualizarTipoDesparasitante(Long id, TipoDesparasitanteRequest request);
    void cambiarEstadoTipoVacuna(Long id, boolean activo);
    void cambiarEstadoTipoDesparasitante(Long id, boolean activo);
    void eliminarTipoVacuna(Long id);
    void eliminarTipoDesparasitante(Long id);
}
