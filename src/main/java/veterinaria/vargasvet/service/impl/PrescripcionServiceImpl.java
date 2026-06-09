package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.Prescripcion;
import veterinaria.vargasvet.domain.enums.EstadoConsulta;
import veterinaria.vargasvet.dto.request.PrescripcionRequest;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.mapper.ConsultaMapper;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PrescripcionRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.PrescripcionService;

import java.time.LocalDate;
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

        if (consulta.getEstado() == EstadoConsulta.CERRADA && !puedeModificarRecetaCerrada()) {
            throw new IllegalArgumentException("No se puede agregar recetas a una consulta cerrada");
        }

        Prescripcion prescripcion = new Prescripcion();
        prescripcion.setConsulta(consulta);
        mapRequestToEntity(request, prescripcion);
        prescripcion.setCreatedAt(LocalDateTime.now());

        Empleado veterinario = null;
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            veterinario = empleadoRepository.findByUserId(userId).orElse(null);
        }
        if (veterinario == null && consulta.getVeterinario() != null) {
            veterinario = consulta.getVeterinario();
        }
        if (veterinario != null) {
            prescripcion.setVeterinario(veterinario);
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

        if (prescripcion.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarRecetaCerrada()) {
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

        if (prescripcion.getConsulta().getEstado() == EstadoConsulta.CERRADA && !puedeModificarRecetaCerrada()) {
            throw new IllegalArgumentException("No se puede eliminar recetas de una consulta cerrada");
        }

        prescripcionRepository.delete(prescripcion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrescripcionResumenResponse> buscar(String query, Integer companyId, String nombreMascota,
                                                    String numeroMicrochip, String numeroDocumentoApoderado,
                                                    String numeroDocumentoEmpleado, String numeroHc,
                                                    LocalDate fechaDesde, LocalDate fechaHasta,
                                                    int page, int size) {
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
        String nombreMascotaFiltro = (nombreMascota != null && !nombreMascota.isBlank())
                ? "%" + nombreMascota.trim().toLowerCase() + "%" : null;

        LocalDate effectiveDesde = fechaDesde != null ? fechaDesde : LocalDate.of(1, 1, 1);
        LocalDate effectiveHasta = fechaHasta != null ? fechaHasta : LocalDate.of(9999, 12, 31);

        return prescripcionRepository.buscar(
                isSuperAdmin, effectiveCompanyId, queryFiltro,
                nombreMascotaFiltro,
                normalizarFiltro(numeroMicrochip),
                normalizarFiltro(numeroDocumentoApoderado),
                normalizarFiltro(numeroDocumentoEmpleado),
                normalizarFiltro(numeroHc),
                effectiveDesde, effectiveHasta,
                PageRequest.of(page, size))
                .map(consultaMapper::toPrescripcionListResponse);
    }

    private String normalizarFiltro(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
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

    private boolean puedeModificarRecetaCerrada() {
        return SecurityUtils.isSuperAdmin() || SecurityUtils.isAdmin();
    }
}
