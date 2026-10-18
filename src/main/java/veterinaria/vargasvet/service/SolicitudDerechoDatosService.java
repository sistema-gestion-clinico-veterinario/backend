package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.SolicitudDerechoDatos;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;
import veterinaria.vargasvet.domain.enums.TipoDerechoDatos;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.dto.request.CrearSolicitudDerechoRequest;
import veterinaria.vargasvet.dto.request.ResolverSolicitudDerechoRequest;
import veterinaria.vargasvet.dto.response.SolicitudDerechoDatosResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.SolicitudDerechoDatosRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.DispositivoInfo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolicitudDerechoDatosService {

    private static final String MODULO = "Privacidad";
    private static final Set<EstadoSolicitudDerecho> ESTADOS_ADMINISTRABLES = Set.of(
            EstadoSolicitudDerecho.EN_REVISION, EstadoSolicitudDerecho.REQUIERE_INFORMACION,
            EstadoSolicitudDerecho.ATENDIDA, EstadoSolicitudDerecho.RECHAZADA);

    private final SolicitudDerechoDatosRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final CompanyComplianceService complianceService;

    @Transactional
    public SolicitudDerechoDatosResponse crear(CrearSolicitudDerechoRequest request, String ip, String userAgent) {
        Integer userId = exigirUsuario();
        Integer companyId = exigirEmpresa();
        exigirGestionInterna(companyId);
        if (repository.existeAbierta(userId, companyId, request.tipo())) {
            throw new IllegalStateException("Ya existe una solicitud abierta para este derecho");
        }
        if (request.tipo() == TipoDerechoDatos.RECTIFICACION
                && (request.datoRectificar() == null || request.datoRectificar().isBlank())) {
            throw new IllegalArgumentException("Indique el dato que necesita rectificar y el valor correcto");
        }
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        SolicitudDerechoDatos solicitud = new SolicitudDerechoDatos();
        solicitud.setUsuario(usuario);
        solicitud.setCompany(company);
        solicitud.setTipo(request.tipo());
        solicitud.setEstado(EstadoSolicitudDerecho.RECIBIDA);
        solicitud.setDescripcion(limpiar(request.descripcion()));
        solicitud.setDatoRectificar(limpiar(request.datoRectificar()));
        solicitud.setIdentidadVerificada(true);
        solicitud.setMetodoVerificacion("Sesión autenticada y empresa activa");
        solicitud.setCreadaAt(AppClock.now());
        solicitud.setActualizadaAt(AppClock.now());
        solicitud.setFechaLimite(sumarDiasHabiles(AppClock.today(), request.tipo().getDiasHabiles()));
        solicitud.setIpSolicitud(recortar(ip, 64));
        solicitud.setDispositivoSolicitud(recortar(DispositivoInfo.describir(userAgent), 255));
        repository.save(solicitud);
        auditLogService.log(companyId, "SOLICITAR_DERECHO_" + request.tipo(), MODULO,
                "El titular T-" + userId + " registró la solicitud " + solicitud.getId()
                        + "; fecha límite operativa " + solicitud.getFechaLimite());
        return respuesta(solicitud, false);
    }

    @Transactional(readOnly = true)
    public List<SolicitudDerechoDatosResponse> listarMias() {
        Integer companyId = exigirEmpresa();
        exigirGestionInterna(companyId);
        return repository.listarDelTitular(exigirUsuario(), companyId).stream()
                .map(s -> respuesta(s, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudDerechoDatosResponse> listar(EstadoSolicitudDerecho estado) {
        Integer companyId = exigirEmpresa();
        exigirGestionInterna(companyId);
        return repository.listar(companyId, estado).stream().map(s -> respuesta(s, true)).toList();
    }

    @Transactional
    public SolicitudDerechoDatosResponse actualizar(Long id, ResolverSolicitudDerechoRequest request) {
        Integer companyId = exigirEmpresa();
        exigirGestionInterna(companyId);
        if (!ESTADOS_ADMINISTRABLES.contains(request.estado())) {
            throw new IllegalArgumentException("La transición de estado solicitada no está permitida");
        }
        SolicitudDerechoDatos solicitud = repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (esTerminal(solicitud.getEstado())) {
            throw new IllegalStateException("Una solicitud resuelta no puede modificarse");
        }
        if (solicitud.getUsuario().getId().equals(exigirUsuario())) {
            throw new AccessDeniedException("No puede resolver una solicitud sobre sus propios datos");
        }
        if ((request.estado() == EstadoSolicitudDerecho.ATENDIDA
                || request.estado() == EstadoSolicitudDerecho.REQUIERE_INFORMACION)
                && longitud(limpiar(request.respuesta())) < 10) {
            throw new IllegalArgumentException("Registre una respuesta clara de al menos 10 caracteres");
        }
        if (request.estado() == EstadoSolicitudDerecho.RECHAZADA
                && longitud(limpiar(request.motivoRechazo())) < 10) {
            throw new IllegalArgumentException("Registre el fundamento del rechazo (mínimo 10 caracteres)");
        }
        solicitud.setEstado(request.estado());
        solicitud.setRespuesta(limpiar(request.respuesta()));
        solicitud.setMotivoRechazo(request.estado() == EstadoSolicitudDerecho.RECHAZADA
                ? limpiar(request.motivoRechazo()) : null);
        solicitud.setActualizadaAt(AppClock.now());
        if (esTerminal(request.estado())) {
            solicitud.setResueltaAt(AppClock.now());
            solicitud.setResueltaPor(usuarioRepository.getReferenceById(exigirUsuario()));
        }
        repository.save(solicitud);
        auditLogService.log(companyId, "ACTUALIZAR_SOLICITUD_DERECHO_DATOS", MODULO,
                "Solicitud " + id + " pasó a " + request.estado());
        return respuesta(solicitud, true);
    }

    static LocalDate sumarDiasHabiles(LocalDate inicio, int cantidad) {
        LocalDate fecha = inicio;
        int agregados = 0;
        while (agregados < cantidad) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue()) {
                agregados++;
            }
        }
        return fecha;
    }

    private SolicitudDerechoDatosResponse respuesta(SolicitudDerechoDatos s, boolean revelarTitular) {
        Usuario u = s.getUsuario();
        return new SolicitudDerechoDatosResponse(s.getId(), s.getTipo(), s.getEstado(), s.getDescripcion(),
                s.getDatoRectificar(), s.isIdentidadVerificada(), s.getMetodoVerificacion(), s.getCreadaAt(),
                s.getFechaLimite(), !esTerminal(s.getEstado()) && s.getFechaLimite().isBefore(AppClock.today()),
                s.getRespuesta(), s.getMotivoRechazo(), s.getResueltaAt(),
                revelarTitular ? nombre(u) : null, revelarTitular ? u.getEmail() : null);
    }

    private boolean esTerminal(EstadoSolicitudDerecho estado) {
        return estado == EstadoSolicitudDerecho.ATENDIDA || estado == EstadoSolicitudDerecho.RECHAZADA;
    }

    private Integer exigirUsuario() {
        Integer id = SecurityUtils.getCurrentUserId();
        if (id == null) throw new AccessDeniedException("Se requiere una sesión autenticada");
        return id;
    }

    private Integer exigirEmpresa() {
        Integer id = SecurityUtils.getCurrentCompanyId();
        if (id == null) throw new AccessDeniedException("Seleccione una empresa");
        return id;
    }

    private String limpiar(String texto) { return texto == null || texto.isBlank() ? null : texto.trim(); }
    private String recortar(String texto, int maximo) { return texto == null ? null : texto.substring(0, Math.min(texto.length(), maximo)); }
    private int longitud(String texto) { return texto == null ? 0 : texto.length(); }
    private String nombre(Usuario u) { return ((u.getNombre() == null ? "" : u.getNombre()) + " "
            + (u.getApellido() == null ? "" : u.getApellido())).trim(); }
    private void exigirGestionInterna(Integer companyId) {
        if (!complianceService.gestionadoPorSoftvet(companyId, ControlCumplimientoClinica.DERECHOS_DATOS))
            throw new IllegalStateException("La clínica atiende estos derechos mediante el canal informado en su aviso de privacidad");
    }
}
