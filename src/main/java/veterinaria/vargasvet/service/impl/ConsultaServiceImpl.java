package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.enums.EstadoCita;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.dto.request.CerrarConsultaRequest;
import veterinaria.vargasvet.dto.request.ConsultaRequest;
import veterinaria.vargasvet.dto.response.ConsultaResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.ConsultaService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final MascotaRepository mascotaRepository;
    private final CitaRepository citaRepository;
    private final ConsultaMapper consultaMapper;
    private final veterinaria.vargasvet.service.AuditLogService auditLogService;
    @org.springframework.beans.factory.annotation.Autowired
    private veterinaria.vargasvet.repository.ControlPreventivoRepository controlPreventivoRepository;

    @Override
    @Transactional
    public ConsultaResponse updateConsulta(Long id, ConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + id));

        if (!consulta.getVersion().equals(request.getVersion())) {
            throw new IllegalArgumentException("La consulta ha sido modificada por otro usuario. Por favor, refresque la página.");
        }

        boolean puedeModificarPorPermiso = SecurityUtils.hasAuthority("CLINICAL_RECORD_MANAGE");
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

            if (!esVeterinarioAsignado && !puedeModificarPorPermiso) {
                throw new IllegalArgumentException("Solo el veterinario asignado a esta consulta puede modificarla");
            }
        }

        if (consulta.getEstado() == EstadoConsulta.CERRADA && !SecurityUtils.isAdmin() && !SecurityUtils.isSuperAdmin() && !puedeModificarPorPermiso) {
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
        if (consulta.getCita() != null && consulta.getCita().getMotivoCita() != null && !consulta.getCita().getMotivoCita().isBlank()) {
            consulta.setMotivoConsulta(consulta.getCita().getMotivoCita());
        }

        HistoriaClinica hc = consulta.getHistoriaClinica();
        if (request.getAntecedentesEnfermedades() != null) hc.setEnfermedades(request.getAntecedentesEnfermedades());
        if (request.getAntecedentesProcedimientos() != null) hc.setProcedimientos(request.getAntecedentesProcedimientos());
        if (request.getAntecedentesPersonales() != null) hc.setAntecedentesPersonales(request.getAntecedentesPersonales());
        if (request.getAntecedentesFamiliares() != null) hc.setAntecedentesFamiliares(request.getAntecedentesFamiliares());
        if (request.getGrupoSanguineo() != null) hc.setGrupoSanguineo(request.getGrupoSanguineo());
        historiaClinicaRepository.save(hc);

        Consulta savedConsulta = consultaRepository.saveAndFlush(consulta);

        auditLogService.log(
            "ACTUALIZAR_CONSULTA",
            "Consultas",
            "Se actualizaron los datos clínicos de la consulta de la mascota " + consulta.getHistoriaClinica().getMascota().getNombreCompleto() + " atendida por " + (consulta.getVeterinario().getUser() != null ? (consulta.getVeterinario().getUser().getNombre() + " " + consulta.getVeterinario().getUser().getApellido()) : "sin usuario") + " el " + consulta.getFechaConsulta()
        );

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

        boolean puedeCerrarPorPermiso = SecurityUtils.hasAuthority("CLINICAL_RECORD_MANAGE");
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

            if (!esVeterinarioAsignado && !puedeCerrarPorPermiso) {
                throw new IllegalArgumentException("Solo el veterinario asignado a esta consulta puede cerrarla");
            }
        }

        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("La consulta ya se encuentra cerrada");
        }

        validarCamposObligatorios(consulta);

        consulta.setEstado(EstadoConsulta.CERRADA);
        consulta.setFechaCierre(veterinaria.vargasvet.util.AppClock.now());
        consulta.setCerradoPor(SecurityUtils.getCurrentUserEmail());

        Cita cita = consulta.getCita();
        if (cita != null) {
            cita.setEstado(EstadoCita.COMPLETADA);
            citaRepository.save(cita);
            liberarControlesNoAplicados(cita);
        }

        Consulta savedConsulta = consultaRepository.saveAndFlush(consulta);

        auditLogService.log(
            "CERRAR_CONSULTA",
            "Consultas",
            "Se cerró la consulta de la mascota " + consulta.getHistoriaClinica().getMascota().getNombreCompleto() + " por el veterinario " + consulta.getCerradoPor()
        );

        return consultaMapper.toResponse(savedConsulta);
    }

    private void validarCamposObligatorios(Consulta consulta) {
        if (consulta.getMotivoConsulta() == null || consulta.getMotivoConsulta().isBlank()) {
            throw new IllegalArgumentException("El motivo de consulta es obligatorio para cerrar la consulta");
        }
        if (consulta.getTipoConsulta() == null) {
            throw new IllegalArgumentException("El tipo de consulta es obligatorio para cerrar la consulta");
        }
        if (consulta.getPesoEnConsulta() == null) {
            throw new IllegalArgumentException("El peso del paciente es obligatorio para cerrar la consulta");
        }
        if (consulta.getAnamnesis() == null || consulta.getAnamnesis().isBlank()) {
            throw new IllegalArgumentException("La anamnesis es obligatoria para cerrar la consulta");
        }
    }

    private void liberarControlesNoAplicados(Cita cita) {
        if (controlPreventivoRepository == null) return;
        java.time.LocalDate hoy = veterinaria.vargasvet.util.AppClock.today();
        java.util.List<veterinaria.vargasvet.domain.entity.ControlPreventivo> controles =
                controlPreventivoRepository.findByCitaSuspendeId(cita.getId());
        for (veterinaria.vargasvet.domain.entity.ControlPreventivo control : controles) {
            if (control.getEstado() != veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.SUSPENDIDO_POR_CITA) continue;
            control.setCitaSuspende(null);
            control.setEstado(control.getFechaRecomendada().isBefore(hoy)
                    ? veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.ATRASADO
                    : control.getFechaRecomendada().isEqual(hoy)
                    ? veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PENDIENTE
                    : !control.getFechaRecomendada().isAfter(hoy.plusDays(7))
                    ? veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROXIMO
                    : veterinaria.vargasvet.domain.enums.EstadoControlPreventivo.PROGRAMADO);
            control.setEstadoModificadoPor(SecurityUtils.getCurrentUserEmail());
            control.setFechaModificacionEstado(veterinaria.vargasvet.util.AppClock.now());
            control.setUpdatedBy(SecurityUtils.getCurrentUserEmail());
        }
        controlPreventivoRepository.saveAll(controles);
    }
}
