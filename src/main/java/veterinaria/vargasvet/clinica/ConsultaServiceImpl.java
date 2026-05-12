package veterinaria.vargasvet.clinica;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.citas.Cita;
import veterinaria.vargasvet.clinica.Consulta;
import veterinaria.vargasvet.clinica.HistoriaClinica;
import veterinaria.vargasvet.shared.EstadoCita;
import veterinaria.vargasvet.shared.EstadoConsulta;
import veterinaria.vargasvet.clinica.CerrarConsultaRequest;
import veterinaria.vargasvet.clinica.ConsultaRequest;
import veterinaria.vargasvet.clinica.ConsultaResponse;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.clinica.ConsultaMapper;
import veterinaria.vargasvet.citas.CitaRepository;
import veterinaria.vargasvet.clinica.ConsultaRepository;
import veterinaria.vargasvet.clinica.HistoriaClinicaRepository;
import veterinaria.vargasvet.pacientes.MascotaRepository;
import veterinaria.vargasvet.shared.SecurityUtils;
import veterinaria.vargasvet.clinica.ConsultaService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ConsultaMapper consultaMapper;

    @Override
    @Transactional
    public ConsultaResponse updateConsulta(Long id, ConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));

        if (!consulta.getVersion().equals(request.getVersion())) {
            throw new IllegalArgumentException("La consulta ha sido modificada por otro usuario. Por favor, refresque la página.");
        }

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

        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("No se puede modificar una consulta cerrada");
        }

        if (request.getTipoConsulta() != null) consulta.setTipoConsulta(request.getTipoConsulta());
        if (request.getPesoEnConsulta() != null) {
            consulta.setPesoEnConsulta(request.getPesoEnConsulta());
            consulta.getHistoriaClinica().getMascota().setPeso(request.getPesoEnConsulta());
            mascotaRepository.save(consulta.getHistoriaClinica().getMascota());
        }
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
        if (request.getIndicacionesReceta() != null) consulta.setIndicacionesReceta(request.getIndicacionesReceta());
        if (request.getMotivoConsulta() != null && !request.getMotivoConsulta().isBlank()) {
            consulta.setMotivoConsulta(request.getMotivoConsulta());
        }

        HistoriaClinica hc = consulta.getHistoriaClinica();
        if (request.getAntecedentesEnfermedades() != null) hc.setEnfermedades(request.getAntecedentesEnfermedades());
        if (request.getAntecedentesProcedimientos() != null) hc.setProcedimientos(request.getAntecedentesProcedimientos());
        if (request.getAntecedentesPersonales() != null) hc.setAntecedentesPersonales(request.getAntecedentesPersonales());
        if (request.getAntecedentesFamiliares() != null) hc.setAntecedentesFamiliares(request.getAntecedentesFamiliares());
        if (request.getGrupoSanguineo() != null) hc.setGrupoSanguineo(request.getGrupoSanguineo());
        historiaClinicaRepository.save(hc);

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

    @Override
    @Transactional
    public ConsultaResponse cerrarConsulta(Long id, CerrarConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));

        if (!consulta.getVersion().equals(request.getVersion())) {
            throw new IllegalArgumentException("La consulta ha sido modificada por otro usuario. Por favor, refresque la página.");
        }

        if (!SecurityUtils.isSuperAdmin() && !SecurityUtils.isAdmin()) {
            Integer currentCompanyId = SecurityUtils.getCurrentCompanyId();
            if (consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany() == null ||
                !consulta.getHistoriaClinica().getMascota().getApoderado().getUser().getCompany().getId().equals(currentCompanyId)) {
                throw new IllegalArgumentException("No tienes permiso para cerrar esta consulta");
            }

            Integer currentUserId = SecurityUtils.getCurrentUserId();
            boolean esVeterinarioAsignado = consulta.getVeterinario() != null &&
                    consulta.getVeterinario().getUser() != null &&
                    consulta.getVeterinario().getUser().getId().equals(currentUserId);

            if (!esVeterinarioAsignado) {
                throw new IllegalArgumentException("Solo el veterinario asignado a esta consulta puede cerrarla");
            }
        }

        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("La consulta ya se encuentra cerrada");
        }

        validarCamposObligatorios(consulta);

        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaCierre(LocalDateTime.now());
        consulta.setCerradoPor(SecurityUtils.getCurrentUserEmail());

        Cita cita = consulta.getCita();
        if (cita != null) {
            cita.setEstado(EstadoCita.COMPLETADA);
            citaRepository.save(cita);
        }

        Consulta savedConsulta = consultaRepository.save(consulta);
        return consultaMapper.toResponse(savedConsulta);
    }

    private void validarCamposObligatorios(Consulta consulta) {
        if (consulta.getMotivoConsulta() == null || consulta.getMotivoConsulta().isBlank()) {
            throw new IllegalArgumentException("El motivo de consulta es obligatorio para cerrar la historia clínica");
        }
        if (consulta.getTipoConsulta() == null) {
            throw new IllegalArgumentException("El tipo de consulta es obligatorio para cerrar la historia clínica");
        }
        if (consulta.getPesoEnConsulta() == null) {
            throw new IllegalArgumentException("El peso del paciente es obligatorio para cerrar la historia clínica");
        }
        if (consulta.getAnamnesis() == null || consulta.getAnamnesis().isBlank()) {
            throw new IllegalArgumentException("La anamnesis es obligatoria para cerrar la historia clínica");
        }
    }
}
