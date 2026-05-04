package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ConsultaService;

@Service
@RequiredArgsConstructor
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final ConsultaMapper consultaMapper;

    @Override
    @Transactional
    public ConsultaResponse updateConsulta(Long id, ConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany() == null ||
                !consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para modificar esta consulta");
            }

            Integer currentUserId = SecurityUtils.getCurrentUserId();
            boolean esVeterinarioAsignado = consulta.getVeterinario() != null &&
                    consulta.getVeterinario().getUser() != null &&
                    consulta.getVeterinario().getUser().getId().equals(currentUserId);

            if (!esVeterinarioAsignado) {
                throw new IllegalArgumentException("Solo el veterinario asignado a esta consulta puede modificarla");
            }
        }

        if (request.getPesoEnConsulta() != null) consulta.setPesoEnConsulta(request.getPesoEnConsulta());
        if (request.getTemperatura() != null) consulta.setTemperatura(request.getTemperatura());
        if (request.getFrecuenciaCardiaca() != null) consulta.setFrecuenciaCardiaca(request.getFrecuenciaCardiaca());
        if (request.getFrecuenciaRespiratoria() != null) consulta.setFrecuenciaRespiratoria(request.getFrecuenciaRespiratoria());
        if (request.getMucosas() != null) consulta.setMucosas(request.getMucosas());
        if (request.getTurgenciaPiel() != null) consulta.setTurgenciaPiel(request.getTurgenciaPiel());
        if (request.getVacunacionAlDia() != null) consulta.setVacunacionAlDia(request.getVacunacionAlDia());
        if (request.getDesparasitacionAlDia() != null) consulta.setDesparasitacionAlDia(request.getDesparasitacionAlDia());
        if (request.getAnamnesis() != null) consulta.setAnamnesis(request.getAnamnesis());
        if (request.getExamenFisico() != null) consulta.setExamenFisico(request.getExamenFisico());
        if (request.getObservaciones() != null) consulta.setObservaciones(request.getObservaciones());
        if (request.getMotivoConsulta() != null && !request.getMotivoConsulta().trim().isEmpty()) {
            consulta.setMotivoConsulta(request.getMotivoConsulta());
        }

        if (request.getAntecedentesEnfermedades() != null || request.getAntecedentesProcedimientos() != null) {
            HistoriaClinica hc = consulta.getHistoriaClinica();
            if (request.getAntecedentesEnfermedades() != null) {
                hc.setEnfermedades(request.getAntecedentesEnfermedades());
            }
            if (request.getAntecedentesProcedimientos() != null) {
                hc.setProcedimientos(request.getAntecedentesProcedimientos());
            }
            historiaClinicaRepository.save(hc);
        }

        Consulta savedConsulta = consultaRepository.save(consulta);
        return consultaMapper.toResponse(savedConsulta);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultaResponse getConsultaById(Long id) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany() == null ||
                !consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para ver esta consulta");
            }
        }

        return consultaMapper.toResponse(consulta);
    }
}
