package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Consulta;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.response.ArchivoClinicoResponse;
import veterinaria.vargasvet.dto.response.ConsultaResumenResponse;
import veterinaria.vargasvet.dto.response.DiagnosticoResumenResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaDetalleResponse;
import veterinaria.vargasvet.dto.response.HistoriaClinicaListResponse;
import veterinaria.vargasvet.dto.response.PrescripcionResumenResponse;
import veterinaria.vargasvet.dto.response.TratamientoResumenResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ConsultaRepository;
import veterinaria.vargasvet.repository.HistoriaClinicaRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.service.HistoriaClinicaService;
import veterinaria.vargasvet.service.impl.ArchivoClinicoServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaClinicaRepository;
    private final ConsultaRepository consultaRepository;
    private final ArchivoClinicoServiceImpl archivoClinicoService;

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriaClinicaListResponse> buscar(String numeroHc, String nombrePaciente, String nombrePropietario,
                                                    LocalDate fechaDesde, LocalDate fechaHasta, int page, int size) {
        boolean isSuperAdmin = SecurityUtils.isSuperAdmin();
        Integer companyId = SecurityUtils.getCurrentCompanyId();

        String hcFiltro = (numeroHc != null && !numeroHc.isBlank()) ? numeroHc.trim() : null;
        String pacienteFiltro = (nombrePaciente != null && !nombrePaciente.isBlank()) ? "%" + nombrePaciente.trim().toLowerCase() + "%" : null;
        String propietarioFiltro = (nombrePropietario != null && !nombrePropietario.isBlank()) ? "%" + nombrePropietario.trim().toLowerCase() + "%" : null;
        String desdeStr = fechaDesde != null ? fechaDesde.toString() + " 00:00:00" : null;
        String hastaStr = fechaHasta != null ? fechaHasta.toString() + " 23:59:59" : null;

        Page<HistoriaClinica> pageHc = historiaClinicaRepository.buscar(
                isSuperAdmin, companyId, hcFiltro, pacienteFiltro, propietarioFiltro,
                desdeStr, hastaStr,
                PageRequest.of(page, size, Sort.unsorted()));

        List<Long> ids = pageHc.getContent().stream().map(HistoriaClinica::getId).toList();

        Map<Long, LocalDateTime> ultimasFechas = ids.isEmpty() ? Map.of() :
                consultaRepository.findUltimasFechasConsulta(ids).stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (LocalDateTime) row[1]));

        return pageHc.map(hc -> toListResponse(hc, ultimasFechas.get(hc.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaDetalleResponse getPorMascota(Long mascotaId) {
        HistoriaClinica hc = historiaClinicaRepository.findByMascotaId(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Historia clínica no encontrada para la mascota con ID: " + mascotaId));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (hc.getMascota().getApoderado().getUser().getCompany() == null ||
                !hc.getMascota().getApoderado().getUser().getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("No tienes permiso para ver esta historia clínica");
            }
        }

        return toDetalleResponse(hc);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinicaDetalleResponse getDetalle(Long id) {
        HistoriaClinica hc = historiaClinicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historia clínica no encontrada con ID: " + id));

        if (!SecurityUtils.isSuperAdmin()) {
            Integer companyId = SecurityUtils.getCurrentCompanyId();
            if (hc.getMascota().getApoderado().getUser().getCompany() == null ||
                !hc.getMascota().getApoderado().getUser().getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("No tienes permiso para ver esta historia clínica");
            }
        }

        return toDetalleResponse(hc);
    }

    private HistoriaClinicaListResponse toListResponse(HistoriaClinica hc, LocalDateTime fechaUltimaConsulta) {
        HistoriaClinicaListResponse response = new HistoriaClinicaListResponse();
        response.setId(hc.getId());
        response.setNumeroHc(hc.getNumeroHc());
        response.setActiva(hc.getActiva());
        response.setFechaUltimaConsulta(fechaUltimaConsulta);

        Mascota mascota = hc.getMascota();
        if (mascota != null) {
            response.setMascotaId(mascota.getId());
            response.setMascotaNombre(mascota.getNombreCompleto());
            response.setEspecie(mascota.getEspecie() != null ? mascota.getEspecie().name() : mascota.getOtraEspecie());
            response.setRaza(mascota.getRaza());
            if (mascota.getApoderado() != null) {
                Usuario user = mascota.getApoderado().getUser();
                if (user != null) {
                    response.setPropietarioNombre(user.getNombre() + " " + user.getApellido());
                }
            }
        }

        return response;
    }

    private HistoriaClinicaDetalleResponse toDetalleResponse(HistoriaClinica hc) {
        HistoriaClinicaDetalleResponse response = new HistoriaClinicaDetalleResponse();
        response.setId(hc.getId());
        response.setNumeroHc(hc.getNumeroHc());
        response.setActiva(hc.getActiva());
        response.setEnfermedades(hc.getEnfermedades());
        response.setProcedimientos(hc.getProcedimientos());
        response.setAntecedentesPersonales(hc.getAntecedentesPersonales());
        response.setAntecedentesFamiliares(hc.getAntecedentesFamiliares());
        response.setGrupoSanguineo(hc.getGrupoSanguineo());

        Mascota mascota = hc.getMascota();
        if (mascota != null) {
            response.setMascotaId(mascota.getId());
            response.setMascotaNombre(mascota.getNombreCompleto());
            response.setEspecie(mascota.getEspecie() != null ? mascota.getEspecie().name() : mascota.getOtraEspecie());
            response.setRaza(mascota.getRaza());
            response.setSexo(mascota.getSexo() != null ? mascota.getSexo().name() : null);
            response.setColor(mascota.getColor());
            response.setSenasParticulares(mascota.getSenasParticulares());
            response.setEsterilizado(mascota.getEsterilizado());

            if (mascota.getFechaNacimiento() != null) {
                response.setEdadAproximadaMeses((int) ChronoUnit.MONTHS.between(mascota.getFechaNacimiento(), LocalDate.now()));
            }

            if (mascota.getApoderado() != null) {
                response.setApoderadoId(mascota.getApoderado().getId());
                Usuario user = mascota.getApoderado().getUser();
                if (user != null) {
                    response.setPropietarioNombre(user.getNombre() + " " + user.getApellido());
                    response.setPropietarioTelefono(user.getTelefono());
                    response.setPropietarioDireccion(user.getDireccion());
                }
            }
        }

        List<ConsultaResumenResponse> consultas = hc.getConsultas().stream()
                .sorted(Comparator.comparing(Consulta::getFechaConsulta).reversed())
                .map(this::toConsultaResumen)
                .toList();

        response.setConsultas(consultas);
        return response;
    }

    private ConsultaResumenResponse toConsultaResumen(Consulta consulta) {
        ConsultaResumenResponse response = new ConsultaResumenResponse();
        response.setId(consulta.getId());
        response.setFechaConsulta(consulta.getFechaConsulta());
        response.setMotivoConsulta(consulta.getMotivoConsulta());
        response.setTipoConsulta(consulta.getTipoConsulta() != null ? consulta.getTipoConsulta().name() : null);
        response.setEstado(consulta.getEstado() != null ? consulta.getEstado().name() : null);

        if (consulta.getVeterinario() != null && consulta.getVeterinario().getUser() != null) {
            Usuario user = consulta.getVeterinario().getUser();
            response.setVeterinarioNombre(user.getNombre() + " " + user.getApellido());
        }

        response.setPesoEnConsulta(consulta.getPesoEnConsulta());
        response.setTemperatura(consulta.getTemperatura());
        response.setFrecuenciaCardiaca(consulta.getFrecuenciaCardiaca());
        response.setFrecuenciaRespiratoria(consulta.getFrecuenciaRespiratoria());
        response.setMucosas(consulta.getMucosas());
        response.setTurgenciaPiel(consulta.getTurgenciaPiel());
        response.setVacunacionAlDia(consulta.getVacunacionAlDia());
        response.setDesparasitacionAlDia(consulta.getDesparasitacionAlDia());
        response.setAnamnesis(consulta.getAnamnesis());
        response.setExamenFisico(consulta.getExamenFisico());
        response.setObservaciones(consulta.getObservaciones());

        response.setDiagnosticos(consulta.getDiagnosticos().stream().map(d -> {
            DiagnosticoResumenResponse dr = new DiagnosticoResumenResponse();
            dr.setId(d.getId());
            dr.setNombre(d.getNombre());
            dr.setCodigoCIE(d.getCodigoCIE());
            dr.setDescripcion(d.getDescripcion());
            dr.setTipo(d.getTipo() != null ? d.getTipo().name() : null);
            dr.setEstado(d.getEstado() != null ? d.getEstado().name() : null);
            return dr;
        }).toList());

        response.setTratamientos(consulta.getTratamientos().stream().map(t -> {
            TratamientoResumenResponse tr = new TratamientoResumenResponse();
            tr.setId(t.getId());
            tr.setNombre(t.getNombre());
            tr.setDescripcion(t.getDescripcion());
            tr.setFechaInicio(t.getFechaInicio());
            tr.setFechaFin(t.getFechaFin());
            tr.setEstado(t.getEstado() != null ? t.getEstado().name() : null);
            return tr;
        }).toList());

        response.setPrescripciones(consulta.getPrescripciones().stream().map(p -> {
            PrescripcionResumenResponse pr = new PrescripcionResumenResponse();
            pr.setId(p.getId());
            pr.setMedicamento(p.getMedicamento());
            pr.setPrincipioActivo(p.getPrincipioActivo());
            pr.setDosis(p.getDosis());
            pr.setFrecuencia(p.getFrecuencia());
            pr.setDuracionDias(p.getDuracionDias());
            pr.setViaAdministracion(p.getViaAdministracion());
            pr.setInstrucciones(p.getInstrucciones());
            pr.setFechaInicio(p.getFechaInicio());
            pr.setFechaFin(p.getFechaFin());
            return pr;
        }).toList());

        response.setArchivos(consulta.getArchivos().stream()
                .map(archivoClinicoService::toResponse)
                .toList());

        return response;
    }
}
