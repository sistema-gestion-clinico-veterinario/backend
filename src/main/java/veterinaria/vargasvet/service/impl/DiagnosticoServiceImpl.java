package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Diagnostico;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.EstadoDiagnostico;
import veterinaria.vargasvet.dto.request.DiagnosticoRequest;
import veterinaria.vargasvet.dto.response.DiagnosticoResumenResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.DiagnosticoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.DiagnosticoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosticoServiceImpl implements DiagnosticoService {

    private final DiagnosticoRepository diagnosticoRepository;
    private final ConsultaRepository consultaRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public DiagnosticoResumenResponse crear(Long consultaId, DiagnosticoRequest request) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));

        verificarPertenenciaEmpresa(consulta);

        if (consulta.getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden agregar diagnósticos a una consulta cerrada");
        }

        Diagnostico diagnostico = new Diagnostico();
        diagnostico.setConsulta(consulta);
        mapRequestToEntity(request, diagnostico);

        Diagnostico saved = diagnosticoRepository.save(diagnostico);

        auditLogService.log(companyIdDe(consulta), "CREAR_DIAGNOSTICO", "Historias Clínicas",
                "Se registró el diagnóstico " + saved.getNombre() + " para la mascota " + nombreMascotaDe(consulta));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticoResumenResponse> listarPorConsulta(Long consultaId) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));
        verificarPertenenciaEmpresa(consulta);
        return diagnosticoRepository.findByConsultaIdOrderById(consultaId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiagnosticoResumenResponse> listarPorMascota(Long mascotaId) {
        List<Diagnostico> diagnosticos = diagnosticoRepository.findByMascotaIdOrderByFechaConsultaDesc(mascotaId);
        if (!diagnosticos.isEmpty()) {
            verificarPertenenciaEmpresa(diagnosticos.get(0).getConsulta());
        }
        return diagnosticos.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DiagnosticoResumenResponse actualizar(Long id, DiagnosticoRequest request) {
        Diagnostico diagnostico = diagnosticoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(diagnostico.getConsulta());

        if (diagnostico.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden modificar diagnósticos de una consulta cerrada");
        }

        mapRequestToEntity(request, diagnostico);
        Diagnostico saved = diagnosticoRepository.save(diagnostico);

        auditLogService.log(companyIdDe(saved.getConsulta()), "ACTUALIZAR_DIAGNOSTICO", "Historias Clínicas",
                "Se actualizó el diagnóstico " + saved.getNombre() + " de la mascota " + nombreMascotaDe(saved.getConsulta()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public DiagnosticoResumenResponse cambiarEstado(Long id, EstadoDiagnostico nuevoEstado) {
        Diagnostico diagnostico = diagnosticoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(diagnostico.getConsulta());

        // A diferencia de crear/editar/eliminar, cambiar el estado de seguimiento (ej. marcar
        // "Resuelto") debe poder hacerse aunque la consulta de origen ya esté cerrada - es
        // justamente el caso de uso: seguimiento en visitas posteriores a esa consulta.
        diagnostico.setEstado(nuevoEstado);
        Diagnostico saved = diagnosticoRepository.save(diagnostico);

        auditLogService.log(companyIdDe(saved.getConsulta()), "CAMBIAR_ESTADO_DIAGNOSTICO", "Historias Clínicas",
                "Se cambió el estado del diagnóstico " + saved.getNombre() + " a " + nuevoEstado
                        + " para la mascota " + nombreMascotaDe(saved.getConsulta()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Diagnostico diagnostico = diagnosticoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnóstico no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(diagnostico.getConsulta());

        if (diagnostico.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden eliminar diagnósticos de una consulta cerrada");
        }

        Integer companyId = companyIdDe(diagnostico.getConsulta());
        String nombre = diagnostico.getNombre();
        String mascota = nombreMascotaDe(diagnostico.getConsulta());

        diagnosticoRepository.delete(diagnostico);

        auditLogService.log(companyId, "ELIMINAR_DIAGNOSTICO", "Historias Clínicas",
                "Se eliminó el diagnóstico " + nombre + " de la mascota " + mascota);
    }

    private DiagnosticoResumenResponse toResponse(Diagnostico d) {
        DiagnosticoResumenResponse dr = new DiagnosticoResumenResponse();
        dr.setId(d.getId());
        dr.setNombre(d.getNombre());
        dr.setCodigoCIE(d.getCodigoCIE());
        dr.setDescripcion(d.getDescripcion());
        dr.setTipo(d.getTipo() != null ? d.getTipo().name() : null);
        dr.setEstado(d.getEstado() != null ? d.getEstado().name() : null);
        dr.setFechaProximoControl(d.getFechaProximoControl());
        if (d.getConsulta() != null) {
            dr.setConsultaId(d.getConsulta().getId());
            dr.setFechaConsulta(d.getConsulta().getFechaConsulta() != null ? d.getConsulta().getFechaConsulta().toLocalDate() : null);
            dr.setVeterinarioNombre(nombreVeterinario(d.getConsulta()));
        }
        return dr;
    }

    private String nombreVeterinario(Consulta consulta) {
        if (consulta.getVeterinario() == null || consulta.getVeterinario().getUser() == null) return null;
        return consulta.getVeterinario().getUser().getNombre() + " " + consulta.getVeterinario().getUser().getApellido();
    }

    private void mapRequestToEntity(DiagnosticoRequest request, Diagnostico diagnostico) {
        diagnostico.setNombre(request.getNombre());
        diagnostico.setCodigoCIE(request.getCodigoCIE());
        diagnostico.setDescripcion(request.getDescripcion());
        diagnostico.setTipo(request.getTipo());
        diagnostico.setEstado(request.getEstado());
        diagnostico.setFechaProximoControl(request.getFechaProximoControl());
    }

    private boolean puedeModificarConsultaCerrada() {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin();
    }

    private Integer companyIdDe(Consulta consulta) {
        if (consulta.getHistoriaClinica() == null || consulta.getHistoriaClinica().getMascota() == null
                || consulta.getHistoriaClinica().getMascota().getApoderado() == null
                || consulta.getHistoriaClinica().getMascota().getApoderado().getUser() == null
                || consulta.getHistoriaClinica().getMascota().getApoderado().getCompany() == null) {
            return null;
        }
        return consulta.getHistoriaClinica().getMascota().getApoderado().getCompany().getId();
    }

    private String nombreMascotaDe(Consulta consulta) {
        if (consulta.getHistoriaClinica() == null || consulta.getHistoriaClinica().getMascota() == null) {
            return "desconocida";
        }
        return consulta.getHistoriaClinica().getMascota().getNombreCompleto();
    }

    private void verificarPertenenciaEmpresa(Consulta consulta) {
        if (SecurityUtils.isSuperAdmin()) {
            return;
        }
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        Integer consultaCompanyId = companyIdDe(consulta);
        if (consultaCompanyId == null || !consultaCompanyId.equals(companyId)) {
            throw new IllegalArgumentException("No tienes permiso para operar sobre una consulta de otra empresa");
        }
    }
}
