package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import veterinaria.vargasvet.domain.entity.SolicitudCambioCumplimiento;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento;
import veterinaria.vargasvet.domain.enums.ModoGestionCumplimiento;
import veterinaria.vargasvet.dto.request.ConfigurarCumplimientoClinicaRequest;
import veterinaria.vargasvet.dto.request.ResolverCambioCumplimientoRequest;
import veterinaria.vargasvet.dto.request.SolicitarCambioCumplimientoRequest;
import veterinaria.vargasvet.dto.response.SolicitudCambioCumplimientoResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.SolicitudCambioCumplimientoRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SolicitudCambioCumplimientoService {
    private static final List<EstadoSolicitudCambioCumplimiento> ACTIVAS = List.of(
            EstadoSolicitudCambioCumplimiento.PENDIENTE,
            EstadoSolicitudCambioCumplimiento.EN_REVISION);
    private static final List<EstadoSolicitudCambioCumplimiento> RESUELTAS = List.of(
            EstadoSolicitudCambioCumplimiento.APROBADA,
            EstadoSolicitudCambioCumplimiento.RECHAZADA);
    private final SolicitudCambioCumplimientoRepository repository;
    private final CompanyRepository companyRepository;
    private final UsuarioRepository usuarioRepository;
    private final CompanyComplianceService complianceService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<SolicitudCambioCumplimientoResponse> listarDeMiClinica(Integer companyId) {
        validarEmpresa(companyId);
        return repository.listarPorEmpresa(companyId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudCambioCumplimientoResponse> listarPendientes(Integer companyId) {
        validarEmpresa(companyId);
        return repository.findByCompanyIdAndEstadoInOrderBySolicitadaAtDesc(companyId, ACTIVAS)
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public Page<SolicitudCambioCumplimientoResponse> listarGlobal(Integer companyId,
            ControlCumplimientoClinica control, EstadoSolicitudCambioCumplimiento estado,
            LocalDate desde, LocalDate hasta, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LocalDateTime fechaDesde = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fechaHasta = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        return repository.buscarGlobal(companyId, control, estado, fechaDesde, fechaHasta,
                        PageRequest.of(safePage, safeSize, Sort.by("solicitadaAt").descending()))
                .map(this::response);
    }

    @Transactional(readOnly = true)
    public long contarActivas() {
        return repository.countByEstadoIn(ACTIVAS);
    }

    @Transactional(readOnly = true)
    public List<SolicitudCambioCumplimientoResponse> notificacionesClinica(Integer companyId) {
        validarEmpresa(companyId);
        return repository.findByCompanyIdAndEstadoInAndVistaClinicaAtIsNullOrderByResueltaAtDesc(companyId, RESUELTAS)
                .stream().map(this::response).toList();
    }

    @Transactional
    public SolicitudCambioCumplimientoResponse solicitar(Integer companyId,
                                                          SolicitarCambioCumplimientoRequest request) {
        validarCoherencia(request.control(), request.habilitado(), request.modo(), request.bloqueante());
        var actual = complianceService.resolver(companyId, request.control());
        if (actual.habilitado() == request.habilitado() && actual.modo() == request.modo()
                && actual.bloqueante() == request.bloqueante()) {
            throw new IllegalArgumentException("La configuración solicitada ya se encuentra vigente");
        }
        if (repository.existsByCompanyIdAndControlAndEstadoIn(companyId, request.control(), ACTIVAS)) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para este control");
        }
        String evidencia = limpiar(request.evidenciaReferencia());
        if (request.modo() == ModoGestionCumplimiento.EXTERNO && evidencia == null) {
            throw new IllegalArgumentException("Indica el contrato, documento o enlace que sustenta la gestión externa");
        }
        if (request.vigenteDesde() != null && request.vigenteDesde().isBefore(AppClock.now())) {
            throw new IllegalArgumentException("La vigencia programada no puede estar en el pasado");
        }
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No se identificó al solicitante");

        SolicitudCambioCumplimiento solicitud = new SolicitudCambioCumplimiento();
        solicitud.setCompany(companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada")));
        solicitud.setControl(request.control());
        solicitud.setHabilitadoSolicitado(request.habilitado());
        solicitud.setModoSolicitado(request.modo());
        solicitud.setBloqueanteSolicitado(request.bloqueante());
        solicitud.setMotivo(request.motivo().trim());
        solicitud.setEvidenciaReferencia(evidencia);
        solicitud.setVigenteDesdeSolicitada(request.vigenteDesde());
        solicitud.setSolicitadaPor(usuarioRepository.getReferenceById(userId));
        solicitud.setSolicitadaAt(AppClock.now());
        repository.save(solicitud);

        auditLogService.log(companyId, "SOLICITAR_CAMBIO_CUMPLIMIENTO", "Privacidad",
                request.control() + ": modo solicitado=" + request.modo());
        return response(solicitud);
    }

    @Transactional
    public SolicitudCambioCumplimientoResponse iniciarRevision(Long id) {
        SolicitudCambioCumplimiento solicitud = obtener(id);
        if (solicitud.getEstado() != EstadoSolicitudCambioCumplimiento.PENDIENTE) {
            throw new IllegalStateException("Solo las solicitudes pendientes pueden pasar a revisión");
        }
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No se identificó al administrador de plataforma");
        solicitud.setEstado(EstadoSolicitudCambioCumplimiento.EN_REVISION);
        solicitud.setRevisionPor(usuarioRepository.getReferenceById(userId));
        solicitud.setRevisionAt(AppClock.now());
        repository.save(solicitud);
        auditLogService.log(solicitud.getCompany().getId(), "INICIAR_REVISION_CUMPLIMIENTO", "Privacidad",
                solicitud.getControl() + ": solicitud #" + solicitud.getId());
        return response(solicitud);
    }

    @Transactional
    public SolicitudCambioCumplimientoResponse resolver(Long id, ResolverCambioCumplimientoRequest request) {
        SolicitudCambioCumplimiento solicitud = obtener(id);
        if (solicitud.getEstado() != EstadoSolicitudCambioCumplimiento.EN_REVISION) {
            throw new IllegalStateException("Primero debes marcar la solicitud como en revisión");
        }
        if (request.aprobar() && !request.confirmoRevisionLegal()) {
            throw new IllegalArgumentException("Debes confirmar la revisión legal antes de aprobar el cambio");
        }

        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) throw new IllegalStateException("No se identificó al administrador de plataforma");
        Usuario responsable = usuarioRepository.getReferenceById(userId);

        if (request.aprobar()) {
            complianceService.configurar(solicitud.getCompany().getId(), solicitud.getControl(),
                    new ConfigurarCumplimientoClinicaRequest(
                            solicitud.isHabilitadoSolicitado(), solicitud.getModoSolicitado(),
                            solicitud.isBloqueanteSolicitado(), solicitud.getMotivo(),
                            solicitud.getEvidenciaReferencia(), true, solicitud.getVigenteDesdeSolicitada()));
            solicitud.setEstado(EstadoSolicitudCambioCumplimiento.APROBADA);
        } else {
            solicitud.setEstado(EstadoSolicitudCambioCumplimiento.RECHAZADA);
        }
        solicitud.setResueltaPor(responsable);
        solicitud.setResueltaAt(AppClock.now());
        solicitud.setComentarioResolucion(request.comentario().trim());
        repository.save(solicitud);

        auditLogService.log(solicitud.getCompany().getId(), "RESOLVER_CAMBIO_CUMPLIMIENTO", "Privacidad",
                solicitud.getControl() + ": " + solicitud.getEstado());
        return response(solicitud);
    }

    @Transactional
    public void marcarVistaPorClinica(Integer companyId, Long id) {
        SolicitudCambioCumplimiento solicitud = obtener(id);
        if (!solicitud.getCompany().getId().equals(companyId)) {
            throw new ResourceNotFoundException("Solicitud no encontrada");
        }
        if (!RESUELTAS.contains(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud todavía no tiene una decisión");
        }
        if (solicitud.getVistaClinicaAt() == null) {
            solicitud.setVistaClinicaAt(AppClock.now());
            repository.save(solicitud);
        }
    }

    private SolicitudCambioCumplimiento obtener(Long id) {
        return repository.buscarCompleta(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
    }

    private void validarEmpresa(Integer companyId) {
        if (companyId == null || !companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Empresa no encontrada");
        }
    }

    private void validarCoherencia(ControlCumplimientoClinica control,
                                   boolean habilitado, ModoGestionCumplimiento modo, boolean bloqueante) {
        if (!habilitado && (modo != ModoGestionCumplimiento.NO_APLICA || bloqueante)) {
            throw new IllegalArgumentException("Un control no aplicable debe quedar deshabilitado y no bloqueante");
        }
        if (habilitado && modo == ModoGestionCumplimiento.NO_APLICA) {
            throw new IllegalArgumentException("Un control habilitado debe gestionarse en SoftVet o externamente");
        }
        if (modo == ModoGestionCumplimiento.EXTERNO && bloqueante) {
            throw new IllegalArgumentException("SoftVet no puede bloquear por evidencia gestionada externamente");
        }
        if (control == ControlCumplimientoClinica.RECORDATORIOS_CORREO
                && habilitado && modo != ModoGestionCumplimiento.SOFTVET) {
            throw new IllegalArgumentException("Los recordatorios automáticos requieren gestionar la autorización en SoftVet");
        }
    }

    private SolicitudCambioCumplimientoResponse response(SolicitudCambioCumplimiento s) {
        return new SolicitudCambioCumplimientoResponse(
                s.getId(), s.getCompany().getId(), s.getCompany().getName(), s.getControl(),
                s.isHabilitadoSolicitado(), s.getModoSolicitado(), s.isBloqueanteSolicitado(),
                s.getMotivo(), s.getEvidenciaReferencia(), s.getVigenteDesdeSolicitada(),
                s.getEstado(), nombre(s.getSolicitadaPor()), s.getSolicitadaAt(),
                nombre(s.getResueltaPor()), s.getResueltaAt(), s.getComentarioResolucion(),
                nombre(s.getRevisionPor()), s.getRevisionAt(), s.getVistaClinicaAt());
    }

    private String nombre(Usuario usuario) {
        if (usuario == null) return null;
        String nombre = ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
        return nombre.isBlank() ? usuario.getEmail() : nombre;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
