package veterinaria.vargasvet.clinica;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.clinica.Consulta;
import veterinaria.vargasvet.admin.Empleado;
import veterinaria.vargasvet.clinica.Prescripcion;
import veterinaria.vargasvet.shared.EstadoConsulta;
import veterinaria.vargasvet.clinica.PrescripcionRequest;
import veterinaria.vargasvet.clinica.PrescripcionResumenResponse;
import veterinaria.vargasvet.shared.ResourceNotFoundException;
import veterinaria.vargasvet.clinica.ConsultaMapper;
import veterinaria.vargasvet.clinica.ConsultaRepository;
import veterinaria.vargasvet.admin.EmpleadoRepository;
import veterinaria.vargasvet.clinica.PrescripcionRepository;
import veterinaria.vargasvet.shared.SecurityUtils;
import veterinaria.vargasvet.clinica.PrescripcionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescripcionServiceImpl implements PrescripcionService {

    private final PrescripcionRepository prescripcionRepository;
    private final ConsultaRepository consultaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ConsultaMapper consultaMapper;

    @Override
    @Transactional
    public PrescripcionResumenResponse crear(Long consultaId, PrescripcionRequest request) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId));

        if (consulta.getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("No se puede agregar recetas a una consulta cerrada");
        }

        Prescripcion prescripcion = new Prescripcion();
        prescripcion.setConsulta(consulta);
        mapRequestToEntity(request, prescripcion);
        prescripcion.setCreatedAt(LocalDateTime.now());

        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            empleadoRepository.findByUserId(userId)
                    .ifPresent(prescripcion::setVeterinario);
        }

        return consultaMapper.toPrescripcionResponse(prescripcionRepository.save(prescripcion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescripcionResumenResponse> listarPorConsulta(Long consultaId) {
        if (!consultaRepository.existsById(consultaId)) {
            throw new ResourceNotFoundException("Consulta no encontrada con ID: " + consultaId);
        }
        return prescripcionRepository.findByConsultaIdOrderByCreatedAtAsc(consultaId)
                .stream()
                .map(consultaMapper::toPrescripcionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PrescripcionResumenResponse actualizar(Long id, PrescripcionRequest request) {
        Prescripcion prescripcion = prescripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con ID: " + id));

        if (prescripcion.getConsulta().getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("No se puede modificar recetas de una consulta cerrada");
        }

        mapRequestToEntity(request, prescripcion);
        return consultaMapper.toPrescripcionResponse(prescripcionRepository.save(prescripcion));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Prescripcion prescripcion = prescripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con ID: " + id));

        if (prescripcion.getConsulta().getEstado() == EstadoConsulta.CERRADA) {
            throw new IllegalArgumentException("No se puede eliminar recetas de una consulta cerrada");
        }

        prescripcionRepository.delete(prescripcion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescripcionResumenResponse> buscar(String query, Integer companyId, int page, int size) {
        boolean isSuperAdmin = SecurityUtils.isSuperAdmin();
        Integer effectiveCompanyId;

        if (isSuperAdmin) {
            if (companyId != null) {
                isSuperAdmin = false;
                effectiveCompanyId = companyId;
            } else {
                effectiveCompanyId = -1;
            }
        } else {
            effectiveCompanyId = SecurityUtils.getCurrentCompanyId();
            if (effectiveCompanyId == null) effectiveCompanyId = -1;
        }

        String queryFiltro = (query != null && !query.isBlank())
                ? "%" + query.trim().toLowerCase() + "%" : null;

        return prescripcionRepository.buscar(
                isSuperAdmin, effectiveCompanyId, queryFiltro,
                PageRequest.of(page, size))
                .map(consultaMapper::toPrescripcionListResponse);
    }

    private void mapRequestToEntity(PrescripcionRequest request, Prescripcion prescripcion) {
        prescripcion.setMedicamento(request.getMedicamento());
        prescripcion.setPrincipioActivo(request.getPrincipioActivo());
        prescripcion.setDosis(request.getDosis());
        prescripcion.setFrecuencia(request.getFrecuencia());
        prescripcion.setDuracionDias(request.getDuracionDias());
        prescripcion.setViaAdministracion(request.getViaAdministracion());
        prescripcion.setInstrucciones(request.getInstrucciones());
        prescripcion.setFechaInicio(request.getFechaInicio());
        prescripcion.setFechaFin(request.getFechaFin());
    }
}
