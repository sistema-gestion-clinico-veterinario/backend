package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Tratamiento;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.domain.enums.EstadoTratamiento;
import veterinaria.vargasvet.dto.request.TratamientoRequest;
import veterinaria.vargasvet.dto.response.TratamientoResumenResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.TratamientoRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.AuditLogService;
import veterinaria.vargasvet.service.TratamientoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TratamientoServiceImpl implements TratamientoService {

    private final TratamientoRepository tratamientoRepository;
    private final ConsultaRepository consultaRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public TratamientoResumenResponse crear(Long consultaId, TratamientoRequest request) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));

        verificarPertenenciaEmpresa(consulta);

        if (consulta.getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden agregar tratamientos a una consulta cerrada");
        }

        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setConsulta(consulta);
        mapRequestToEntity(request, tratamiento);

        Tratamiento saved = tratamientoRepository.save(tratamiento);

        auditLogService.log(companyIdDe(consulta), "CREAR_TRATAMIENTO", "Historias Clínicas",
                "Se registró el tratamiento " + saved.getNombre() + " para la mascota " + nombreMascotaDe(consulta));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TratamientoResumenResponse> listarPorConsulta(Long consultaId) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));
        verificarPertenenciaEmpresa(consulta);
        return tratamientoRepository.findByConsultaIdOrderById(consultaId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TratamientoResumenResponse> listarPorMascota(Long mascotaId) {
        List<Tratamiento> tratamientos = tratamientoRepository.findByMascotaIdOrderByFechaConsultaDesc(mascotaId);
        if (!tratamientos.isEmpty()) {
            verificarPertenenciaEmpresa(tratamientos.get(0).getConsulta());
        }
        return tratamientos.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TratamientoResumenResponse actualizar(Long id, TratamientoRequest request) {
        Tratamiento tratamiento = tratamientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tratamiento no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(tratamiento.getConsulta());

        if (tratamiento.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden modificar tratamientos de una consulta cerrada");
        }

        mapRequestToEntity(request, tratamiento);
        Tratamiento saved = tratamientoRepository.save(tratamiento);

        auditLogService.log(companyIdDe(saved.getConsulta()), "ACTUALIZAR_TRATAMIENTO", "Historias Clínicas",
                "Se actualizó el tratamiento " + saved.getNombre() + " de la mascota " + nombreMascotaDe(saved.getConsulta()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TratamientoResumenResponse cambiarEstado(Long id, EstadoTratamiento nuevoEstado) {
        Tratamiento tratamiento = tratamientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tratamiento no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(tratamiento.getConsulta());

        // Igual que en Diagnostico: el seguimiento de un tratamiento (marcarlo Completado,
        // Suspendido, etc.) debe poder hacerse en visitas posteriores a la consulta donde se
        // originó, aunque esa consulta ya este cerrada.
        tratamiento.setEstado(nuevoEstado);
        Tratamiento saved = tratamientoRepository.save(tratamiento);

        auditLogService.log(companyIdDe(saved.getConsulta()), "CAMBIAR_ESTADO_TRATAMIENTO", "Historias Clínicas",
                "Se cambió el estado del tratamiento " + saved.getNombre() + " a " + nuevoEstado
                        + " para la mascota " + nombreMascotaDe(saved.getConsulta()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Tratamiento tratamiento = tratamientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tratamiento no encontrado con ID: " + id));

        verificarPertenenciaEmpresa(tratamiento.getConsulta());

        if (tratamiento.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarConsultaCerrada()) {
            throw new IllegalArgumentException("No se pueden eliminar tratamientos de una consulta cerrada");
        }

        Integer companyId = companyIdDe(tratamiento.getConsulta());
        String nombre = tratamiento.getNombre();
        String mascota = nombreMascotaDe(tratamiento.getConsulta());

        tratamientoRepository.delete(tratamiento);

        auditLogService.log(companyId, "ELIMINAR_TRATAMIENTO", "Historias Clínicas",
                "Se eliminó el tratamiento " + nombre + " de la mascota " + mascota);
    }

    private TratamientoResumenResponse toResponse(Tratamiento t) {
        TratamientoResumenResponse tr = new TratamientoResumenResponse();
        tr.setId(t.getId());
        tr.setNombre(t.getNombre());
        tr.setDescripcion(t.getDescripcion());
        tr.setFechaInicio(t.getFechaInicio());
        tr.setFechaFin(t.getFechaFin());
        tr.setEstado(t.getEstado() != null ? t.getEstado().name() : null);
        if (t.getConsulta() != null) {
            tr.setConsultaId(t.getConsulta().getId());
            tr.setFechaConsulta(t.getConsulta().getFechaConsulta() != null ? t.getConsulta().getFechaConsulta().toLocalDate() : null);
            tr.setVeterinarioNombre(nombreVeterinario(t.getConsulta()));
        }
        return tr;
    }

    private String nombreVeterinario(Consulta consulta) {
        if (consulta.getVeterinario() == null || consulta.getVeterinario().getUser() == null) return null;
        return consulta.getVeterinario().getUser().getNombre() + " " + consulta.getVeterinario().getUser().getApellido();
    }

    private void mapRequestToEntity(TratamientoRequest request, Tratamiento tratamiento) {
        tratamiento.setNombre(request.getNombre());
        tratamiento.setDescripcion(request.getDescripcion());
        tratamiento.setFechaInicio(request.getFechaInicio());
        tratamiento.setFechaFin(request.getFechaFin());
        tratamiento.setEstado(request.getEstado());
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
