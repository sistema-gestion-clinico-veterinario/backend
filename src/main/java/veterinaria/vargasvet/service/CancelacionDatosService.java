package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.SolicitudCancelacionDatos;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.CanalSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.response.CancelacionDatosElegibilidad;
import veterinaria.vargasvet.dto.response.SolicitudCancelacionResponse;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.AuditLogRepository;
import veterinaria.vargasvet.repository.CierreCuentaRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.EntregaAvisoPrivacidadRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.SolicitudCancelacionDatosRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaContactoRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioMembresiaRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Derecho de cancelación del titular (Ley N.° 29733): la persona pide que se eliminen sus datos
 * personales, la clínica (responsable del tratamiento) verifica la identidad, comprueba si existe una
 * obligación de conservar información y, si procede, el sistema anonimiza en el mismo lugar lo que la
 * identifica. No se borran filas: los registros clínicos y comerciales conservan su integridad referencial
 * y dejan de identificar a la persona.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelacionDatosService {

    public static final String CODE_PURPOSE = "CANCELACION_DATOS";
    static final String MODULO_AUDITORIA = "Privacidad";
    static final String PREFIJO_DOCUMENTO = "ANON-";
    static final String DOMINIO_ANONIMO = "@anonimizado.invalid";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarioRepository;
    private final CompanyRepository companyRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioMembresiaRepository membresiaRepository;
    private final UsuarioEmpresaContactoRepository contactoRepository;
    private final UsuarioEmpresaCredencialRepository credencialRepository;
    private final CierreCuentaRepository cierreCuentaRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final EntregaAvisoPrivacidadRepository entregaAvisoRepository;
    private final AuditLogRepository auditLogRepository;
    private final CitaRepository citaRepository;
    private final SolicitudCancelacionDatosRepository solicitudRepository;
    private final PasswordEncoder passwordEncoder;
    private final PetOwnershipService petOwnershipService;
    private final SessionSecurityService sessionSecurityService;
    private final CompanyMembershipService companyMembershipService;
    private final VerificationCodeService verificationCodeService;
    private final SharedRateLimitService sharedRateLimitService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${security.data-cancellation-business-days:10}")
    private int diasHabilesRespuesta;

    @Value("${security.verification-code-validity-minutes:10}")
    private long vigenciaCodigoMinutos;

    // ------------------------------------------------------------------ titular

    @Transactional(readOnly = true)
    public CancelacionDatosElegibilidad elegibilidad() {
        Contexto contexto = cargarContextoTitular();
        boolean requierePassword = contexto.credencial().isPasswordChanged();
        Optional<SolicitudCancelacionDatos> ultima = solicitudRepository
                .findFirstByUsuarioIdAndCompanyIdOrderByIdDesc(contexto.usuario().getId(), contexto.company().getId());
        SolicitudCancelacionResponse vigente = ultima.map(s -> aRespuesta(s, false)).orElse(null);
        if (ultima.isPresent() && ultima.get().getEstado() == EstadoSolicitudCancelacion.PENDIENTE) {
            return new CancelacionDatosElegibilidad(false,
                    "Ya tienes una solicitud en revisión. La clínica te responderá antes del "
                            + ultima.get().getFechaLimite().format(FECHA) + ".", requierePassword, vigente);
        }
        return new CancelacionDatosElegibilidad(true, null, requierePassword, vigente);
    }

    @Transactional
    public long pedirCodigo(String password) {
        Contexto contexto = cargarContextoTitular();
        String clave = contexto.usuario().getId() + ":" + contexto.company().getId();
        sharedRateLimitService.enforce("data-cancellation-request", clave, 3, Duration.ofHours(1));
        exigirSinSolicitudPendiente(contexto);
        if (contexto.credencial().isPasswordChanged()) {
            sharedRateLimitService.enforce("data-cancellation-password", clave, 5, Duration.ofMinutes(15));
            if (password == null || !passwordEncoder.matches(password, contexto.credencial().getPassword())) {
                throw new IllegalArgumentException("La contraseña es incorrecta");
            }
        }
        String codigo = verificationCodeService.issue(contexto.usuario(), contexto.company(), CODE_PURPOSE);
        Map<String, Object> modelo = modeloCorreo(contexto.usuario(), contexto.company());
        modelo.put("code", codigo);
        modelo.put("validityMinutes", vigenciaCodigoMinutos);
        emailService.sendEmailWithRetry(
                emailService.createMail(contexto.usuario().getEmail(), "Código para cancelar tus datos personales", modelo),
                "email/data-cancellation-code-template");
        auditLogService.log(contexto.company().getId(), "SOLICITAR_CODIGO_CANCELACION_DATOS", MODULO_AUDITORIA,
                "El titular " + referencia(contexto.usuario()) + " pidió el código para solicitar la cancelación de sus datos");
        return verificationCodeService.segundosParaPedirOtro(contexto.usuario(), contexto.company(), CODE_PURPOSE);
    }

    @Transactional
    public SolicitudCancelacionResponse solicitar(String codigo, String motivo) {
        Contexto contexto = cargarContextoTitular();
        exigirSinSolicitudPendiente(contexto);
        verificationCodeService.verifyAndConsume(
                contexto.usuario().getId(), contexto.company().getId(), CODE_PURPOSE, codigo);

        SolicitudCancelacionDatos solicitud = nuevaSolicitud(contexto.usuario(), contexto.company(),
                CanalSolicitudCancelacion.TITULAR,
                contexto.credencial().isPasswordChanged() ? "Contraseña y código enviado al correo" : "Código enviado al correo",
                limpiar(motivo), contexto.usuario().getId());
        solicitudRepository.save(solicitud);

        Map<String, Object> modelo = modeloCorreo(contexto.usuario(), contexto.company());
        modelo.put("deadline", solicitud.getFechaLimite().format(FECHA));
        emailService.sendEmailWithRetry(
                emailService.createMail(contexto.usuario().getEmail(), "Recibimos tu solicitud de cancelación de datos", modelo),
                "email/data-cancellation-received-template");
        auditLogService.log(contexto.company().getId(), "SOLICITAR_CANCELACION_DATOS", MODULO_AUDITORIA,
                "El titular " + referencia(contexto.usuario()) + " solicitó la cancelación de sus datos personales (solicitud "
                        + solicitud.getId() + ", respuesta antes del " + solicitud.getFechaLimite().format(FECHA) + ")");
        return aRespuesta(solicitud, false);
    }

    // ------------------------------------------------------------------ administrador de la clínica

    @Transactional(readOnly = true)
    public List<SolicitudCancelacionResponse> listar(EstadoSolicitudCancelacion estado) {
        Integer companyId = empresaDelAdministrador();
        return solicitudRepository.listar(companyId, estado).stream().map(s -> aRespuesta(s, true)).toList();
    }

    @Transactional
    public SolicitudCancelacionResponse registrarPorAdministrador(Long apoderadoId, String metodoVerificacion, String motivo) {
        Integer companyId = empresaDelAdministrador();
        Apoderado apoderado = apoderadoRepository.findById(apoderadoId)
                .filter(a -> a.getCompany() != null && companyId.equals(a.getCompany().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Usuario usuario = apoderado.getUser();
        if (solicitudRepository.existsByUsuarioIdAndCompanyIdAndEstado(usuario.getId(), companyId, EstadoSolicitudCancelacion.PENDIENTE)) {
            throw new IllegalStateException("Este titular ya tiene una solicitud en revisión");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        SolicitudCancelacionDatos solicitud = nuevaSolicitud(usuario, company, CanalSolicitudCancelacion.ADMINISTRADOR,
                "Verificado por la clínica: " + metodoVerificacion.trim(), limpiar(motivo), SecurityUtils.getCurrentUserId());
        solicitudRepository.save(solicitud);

        Map<String, Object> modelo = modeloCorreo(usuario, company);
        modelo.put("deadline", solicitud.getFechaLimite().format(FECHA));
        emailService.sendEmailWithRetry(
                emailService.createMail(usuario.getEmail(), "Recibimos tu solicitud de cancelación de datos", modelo),
                "email/data-cancellation-received-template");
        auditLogService.log(companyId, "REGISTRAR_SOLICITUD_CANCELACION_DATOS", MODULO_AUDITORIA,
                "El administrador registró a nombre del titular " + referencia(usuario) + " una solicitud de cancelación de datos"
                        + " (solicitud " + solicitud.getId() + "). Verificación de identidad: " + solicitud.getMetodoVerificacion());
        return aRespuesta(solicitud, true);
    }

    @Transactional
    public SolicitudCancelacionResponse ejecutar(Long id) {
        Integer companyId = empresaDelAdministrador();
        SolicitudCancelacionDatos solicitud = cargarPendiente(id, companyId);
        Usuario usuario = solicitud.getUsuario();
        if (usuario.getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("No puede resolver una solicitud sobre sus propios datos");
        }
        if (!solicitud.isIdentidadVerificada()) {
            throw new IllegalStateException("No se puede ejecutar sin verificar la identidad del titular");
        }
        Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(usuario.getId(), companyId).orElse(null);
        Optional<String> impedimento = impedimento(usuario, companyId, apoderado);
        if (impedimento.isPresent()) {
            throw new IllegalStateException(impedimento.get());
        }

        String resumen = anonimizar(usuario, solicitud.getCompany(), apoderado);

        solicitud.setEstado(EstadoSolicitudCancelacion.EJECUTADA);
        solicitud.setResueltaAt(AppClock.now());
        solicitud.setResueltaPorUsuarioId(SecurityUtils.getCurrentUserId());
        solicitud.setMotivoTitular(null);
        solicitud.setResumen(resumen);
        solicitudRepository.save(solicitud);

        auditLogService.log(companyId, "EJECUTAR_CANCELACION_DATOS", MODULO_AUDITORIA,
                "Se canceló la información personal del titular " + referencia(usuario) + " (solicitud " + solicitud.getId()
                        + "). " + resumen);
        return aRespuesta(solicitud, true);
    }

    @Transactional
    public SolicitudCancelacionResponse rechazar(Long id, String motivo) {
        Integer companyId = empresaDelAdministrador();
        SolicitudCancelacionDatos solicitud = cargarPendiente(id, companyId);
        String razon = motivo == null ? "" : motivo.trim();
        if (razon.length() < 10) {
            throw new IllegalArgumentException("Indique el motivo del rechazo (mínimo 10 caracteres)");
        }
        solicitud.setEstado(EstadoSolicitudCancelacion.RECHAZADA);
        solicitud.setResueltaAt(AppClock.now());
        solicitud.setResueltaPorUsuarioId(SecurityUtils.getCurrentUserId());
        solicitud.setMotivoResolucion(razon);
        solicitudRepository.save(solicitud);

        Usuario usuario = solicitud.getUsuario();
        Map<String, Object> modelo = modeloCorreo(usuario, solicitud.getCompany());
        modelo.put("reason", razon);
        emailService.sendEmailWithRetry(
                emailService.createMail(usuario.getEmail(), "Respuesta a tu solicitud de cancelación de datos", modelo),
                "email/data-cancellation-rejected-template");
        auditLogService.log(companyId, "RECHAZAR_CANCELACION_DATOS", MODULO_AUDITORIA,
                "Se rechazó la solicitud " + solicitud.getId() + " del titular " + referencia(usuario) + ". Motivo: " + razon);
        return aRespuesta(solicitud, true);
    }

    // ------------------------------------------------------------------ reglas

    Optional<String> impedimento(Usuario usuario, Integer companyId, Apoderado apoderado) {
        if (empleadoRepository.existsByUserIdAndCompanyIdAndEstadoTrue(usuario.getId(), companyId)) {
            return Optional.of("Es personal activo de la clínica. Sus datos están ligados a registros profesionales y laborales "
                    + "y se gestionan por otra vía.");
        }
        if (apoderado == null) {
            return Optional.of("La persona no es cliente de esta clínica.");
        }
        if (yaAnonimizado(apoderado)) {
            return Optional.of("Los datos de esta persona ya fueron cancelados.");
        }
        LocalDateTime ahora = AppClock.now();
        if (citaRepository.existsCitaVigenteByApoderadoId(apoderado.getId(), ahora)) {
            return Optional.of("Tiene citas programadas. Cancélelas o espere a que se realicen antes de ejecutar la cancelación.");
        }
        BigDecimal deuda = citaRepository.saldoPendienteByApoderadoId(apoderado.getId());
        if (deuda != null && deuda.signum() > 0) {
            return Optional.of("Tiene una deuda pendiente de S/ " + deuda.setScale(2, RoundingMode.HALF_UP)
                    + ". La clínica debe conservar sus datos hasta saldarla.");
        }
        return Optional.empty();
    }

    private boolean yaAnonimizado(Apoderado apoderado) {
        return apoderado.getNumeroDocumento() != null && apoderado.getNumeroDocumento().startsWith(PREFIJO_DOCUMENTO);
    }

    private String anonimizar(Usuario usuario, Company company, Apoderado apoderado) {
        LocalDateTime ahora = AppClock.now();
        Integer companyId = company.getId();
        String correoOriginal = usuario.getEmail();
        String nombreOriginal = nombreCompleto(usuario);
        String documentoOriginal = apoderado.getNumeroDocumento();
        String dniOriginal = usuario.getDni();
        String seudonimoCorreo = "titular-" + usuario.getId() + DOMINIO_ANONIMO;
        String seudonimoNombre = "Titular " + usuario.getId();

        Map<String, Object> modelo = modeloCorreo(usuario, company);
        emailService.sendEmailWithRetry(
                emailService.createMail(correoOriginal, "Atendimos tu solicitud de cancelación de datos", modelo),
                "email/data-cancellation-executed-template");

        contactoRepository.findByUsuarioIdAndCompanyId(usuario.getId(), companyId).ifPresent(contacto -> {
            contacto.setTelefono(null);
            contacto.setDireccion(null);
            contacto.setUpdatedAt(ahora);
            contactoRepository.save(contacto);
        });

        apoderado.setNumeroDocumento(PREFIJO_DOCUMENTO + apoderado.getId());
        apoderado.setReferencias(null);
        apoderado.setObservaciones(null);
        apoderado.setEstado(false);
        apoderado.setTipoInactividad(TipoInactividad.BAJA);
        apoderado.setFechaSalida(AppClock.today());
        apoderado.setEstadoModificadoPor("cancelación de datos");
        apoderado.setFechaModificacionEstado(ahora);
        apoderadoRepository.save(apoderado);
        petOwnershipService.syncPets(apoderado, TipoInactividad.BAJA);

        credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), companyId).ifPresent(credencial -> {
            credencial.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            credencial.setPasswordChanged(false);
            credencial.setActivatedWithGoogle(false);
            credencial.setCredentialsVersion(credencial.getCredentialsVersion() + 1L);
            credencial.setUpdatedAt(ahora);
            credencialRepository.save(credencial);
            sessionSecurityService.invalidateSessionsForCredential(credencial);
        });
        sessionSecurityService.invalidateSessionsForCompany(usuario, company);
        passwordResetTokenRepository.deleteByUsuarioAndCompany(usuario, company);

        cierreCuentaRepository.findByUsuarioIdAndCompanyId(usuario.getId(), companyId).forEach(cierre -> {
            cierre.setEstado(EstadoCierreCuenta.PURGADA);
            if (cierre.getPurgadaAt() == null) {
                cierre.setPurgadaAt(ahora);
            }
            cierre.setTokenHash(SecurityTokenUtils.hash(SecurityTokenUtils.generate()));
            cierreCuentaRepository.save(cierre);
        });
        int entregas = entregaAvisoRepository.anonimizarDestino(usuario.getId(), companyId, seudonimoCorreo);

        boolean identidadCompartida = tieneOtrasVinculaciones(usuario, companyId);
        if (!identidadCompartida) {
            emailChangeRequestRepository.deleteByUsuario(usuario);
            usuario.setNombre("Titular");
            usuario.setApellido("anonimizado");
            usuario.setDni(null);
            usuario.setEmail(seudonimoCorreo);
            usuario.setUsername("titular-" + usuario.getId());
            usuario.setActivo(false);
            usuario.setEmailVerified(false);
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
            usuarioRepository.save(usuario);
        }
        companyMembershipService.syncLegacyCompanyField(usuario);

        int auditoria = auditLogRepository.pseudonimizarCorreo(companyId, correoOriginal, seudonimoCorreo)
                + auditLogRepository.reemplazarEnDetalle(companyId, correoOriginal, seudonimoCorreo);
        auditoria += reemplazarSiEsSignificativo(companyId, nombreOriginal, seudonimoNombre);
        auditoria += reemplazarSiEsSignificativo(companyId, documentoOriginal, PREFIJO_DOCUMENTO + apoderado.getId());
        auditoria += reemplazarSiEsSignificativo(companyId, dniOriginal, PREFIJO_DOCUMENTO + apoderado.getId());

        List<String> hechos = new ArrayList<>();
        hechos.add("Se retiraron nombre, documento, correo, teléfono y dirección"
                + (identidadCompartida ? " de esta clínica; la identidad base se conservó porque la persona sigue vinculada a otra empresa" : ""));
        hechos.add("se cerraron sesiones y credenciales");
        hechos.add(entregas + " envíos del aviso de privacidad sin destinatario");
        hechos.add(auditoria + " referencias reemplazadas en la auditoría");
        hechos.add("se conservan, sin identificar a la persona, los registros clínicos y comerciales");
        return String.join("; ", hechos) + ".";
    }

    private int reemplazarSiEsSignificativo(Integer companyId, String valor, String reemplazo) {
        if (valor == null || valor.trim().length() < 6) {
            return 0;
        }
        return auditLogRepository.reemplazarEnDetalle(companyId, valor.trim(), reemplazo);
    }

    private boolean tieneOtrasVinculaciones(Usuario usuario, Integer companyId) {
        boolean otraMembresia = membresiaRepository.findAllActiveByUsuarioId(usuario.getId()).stream()
                .anyMatch(m -> m.getCompany() != null && !companyId.equals(m.getCompany().getId()));
        boolean otroCliente = apoderadoRepository.findAllActiveByUserId(usuario.getId()).stream()
                .anyMatch(a -> a.getCompany() != null && !companyId.equals(a.getCompany().getId()));
        boolean otroPuesto = empleadoRepository.findActiveByUserId(usuario.getId())
                .filter(e -> e.getCompany() != null && !companyId.equals(e.getCompany().getId())).isPresent();
        return otraMembresia || otroCliente || otroPuesto;
    }

    // ------------------------------------------------------------------ apoyo

    private record Contexto(Usuario usuario, Company company, UsuarioEmpresaCredencial credencial, Apoderado apoderado) {}

    private Contexto cargarContextoTitular() {
        Integer usuarioId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (SecurityUtils.isSuperAdmin() || companyId == null || SecurityUtils.getCurrentRolePurpose() != RolePurpose.CLIENT_PORTAL) {
            throw new AccessDeniedException("Solo los clientes pueden solicitar la cancelación de sus datos desde su cuenta. "
                    + "El personal debe pedirla a la administración de la clínica.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        UsuarioEmpresaCredencial credencial = credencialRepository.findByUsuarioIdAndCompanyId(usuarioId, companyId)
                .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));
        Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(usuarioId, companyId)
                .orElseThrow(() -> new IllegalStateException("No eres cliente de esta clínica"));
        return new Contexto(usuario, company, credencial, apoderado);
    }

    private Integer empresaDelAdministrador() {
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null || SecurityUtils.getCurrentRolePurpose() != RolePurpose.COMPANY_ADMIN) {
            throw new AccessDeniedException("Solo el administrador de la clínica gestiona las solicitudes de cancelación");
        }
        return companyId;
    }

    private void exigirSinSolicitudPendiente(Contexto contexto) {
        if (solicitudRepository.existsByUsuarioIdAndCompanyIdAndEstado(
                contexto.usuario().getId(), contexto.company().getId(), EstadoSolicitudCancelacion.PENDIENTE)) {
            throw new IllegalStateException("Ya tienes una solicitud en revisión");
        }
    }

    private SolicitudCancelacionDatos cargarPendiente(Long id, Integer companyId) {
        SolicitudCancelacionDatos solicitud = solicitudRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudCancelacion.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue resuelta");
        }
        return solicitud;
    }

    private SolicitudCancelacionDatos nuevaSolicitud(Usuario usuario, Company company, CanalSolicitudCancelacion canal,
                                                     String metodo, String motivo, Integer registradaPor) {
        LocalDateTime ahora = AppClock.now();
        SolicitudCancelacionDatos solicitud = new SolicitudCancelacionDatos();
        solicitud.setUsuario(usuario);
        solicitud.setCompany(company);
        solicitud.setCanal(canal);
        solicitud.setEstado(EstadoSolicitudCancelacion.PENDIENTE);
        solicitud.setIdentidadVerificada(true);
        solicitud.setMetodoVerificacion(metodo);
        solicitud.setMotivoTitular(motivo);
        solicitud.setRegistradaPorUsuarioId(registradaPor);
        solicitud.setCreadaAt(ahora);
        solicitud.setFechaLimite(sumarDiasHabiles(ahora.toLocalDate(), diasHabilesRespuesta));
        return solicitud;
    }

    static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int sumados = 0;
        while (sumados < dias) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek().getValue() < DayOfWeek.SATURDAY.getValue()) {
                sumados++;
            }
        }
        return fecha;
    }

    private SolicitudCancelacionResponse aRespuesta(SolicitudCancelacionDatos s, boolean paraAdministrador) {
        Usuario usuario = s.getUsuario();
        boolean pendiente = s.getEstado() == EstadoSolicitudCancelacion.PENDIENTE;
        String impedimento = null;
        boolean puedeEjecutarse = false;
        String documento = null;
        if (paraAdministrador) {
            Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(usuario.getId(), s.getCompany().getId()).orElse(null);
            documento = apoderado != null ? apoderado.getNumeroDocumento() : null;
            if (pendiente) {
                Optional<String> bloqueo = impedimento(usuario, s.getCompany().getId(), apoderado);
                impedimento = bloqueo.orElse(null);
                puedeEjecutarse = bloqueo.isEmpty() && s.isIdentidadVerificada();
            }
        }
        boolean vencida = pendiente && AppClock.today().isAfter(s.getFechaLimite());
        return new SolicitudCancelacionResponse(
                s.getId(), s.getEstado(), s.getCanal(), s.isIdentidadVerificada(), s.getMetodoVerificacion(),
                s.getMotivoTitular(), s.getCreadaAt(), s.getFechaLimite(), vencida, s.getResueltaAt(), s.getMotivoResolucion(),
                paraAdministrador ? nombreCompleto(usuario) : null,
                paraAdministrador ? usuario.getEmail() : null,
                documento, puedeEjecutarse, impedimento);
    }

    private String referencia(Usuario usuario) {
        return "T-" + usuario.getId();
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private String nombreCompleto(Usuario usuario) {
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    private Map<String, Object> modeloCorreo(Usuario usuario, Company company) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("nombre", nombreCompleto(usuario));
        modelo.put("companyName", company.getName());
        modelo.put("companyLogo", company.getLogoUrl() != null ? company.getLogoUrl() : "");
        modelo.put("companyEmail", company.getEmail() != null ? company.getEmail() : "");
        modelo.put("companyPhone", company.getPhone() != null ? company.getPhone() : "");
        modelo.put("companyAddress", company.getAddress() != null ? company.getAddress() : "");
        String color = company.getColorPrimario();
        if (color != null && color.matches("#[0-9a-fA-F]{6}")) {
            modelo.put("accentColor", color);
        }
        return modelo;
    }
}
