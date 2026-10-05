package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.CierreCuenta;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Empleado;
import veterinaria.vargasvet.domain.entity.SesionCaja;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.domain.enums.EstadoCierreCuenta;
import veterinaria.vargasvet.domain.enums.EstadoSesionCaja;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.TipoInactividad;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.response.AccountClosureEligibility;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CierreCuentaRepository;
import veterinaria.vargasvet.repository.CitaRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.SesionCajaRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.EmailLinkUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cierre de cuenta por la propia persona, por clínica: corta el acceso a esa empresa sin eliminar sus
 * registros. Durante el plazo de gracia se puede reactivar con el enlace del correo; vencido, se borra el
 * secreto de la credencial. Lo que ocurra en una empresa nunca afecta a la persona en otra.
 */
@Service
@RequiredArgsConstructor
public class AccountClosureService {

    public static final String CODE_PURPOSE = "CIERRE_CUENTA";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarioRepository;
    private final CompanyRepository companyRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final UsuarioEmpresaCredencialRepository credencialRepository;
    private final CierreCuentaRepository cierreCuentaRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final CitaRepository citaRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdministratorProtection administratorProtection;
    private final PetOwnershipService petOwnershipService;
    private final SessionSecurityService sessionSecurityService;
    private final CompanyMembershipService companyMembershipService;
    private final VerificationCodeService verificationCodeService;
    private final SharedRateLimitService sharedRateLimitService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${security.account-closure-grace-days:30}")
    private long graceDays;

    @Value("${security.verification-code-validity-minutes:10}")
    private long codeValidityMinutes;

    @Value("${app.url}")
    private String frontendUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

    private record Context(Usuario usuario, Company company, UsuarioEmpresaCredencial credencial,
                           Empleado empleado, Apoderado apoderado) {}

    @Transactional(readOnly = true)
    public AccountClosureEligibility eligibility() {
        if (SecurityUtils.isSuperAdmin() || SecurityUtils.getCurrentCompanyId() == null) {
            return new AccountClosureEligibility(false, "La cuenta de plataforma no se puede cerrar", false);
        }
        Context context = loadContext();
        boolean requiresPassword = context.credencial() != null && context.credencial().isPasswordChanged();
        try {
            assertCanClose(context);
            return new AccountClosureEligibility(true, null, requiresPassword);
        } catch (AccessDeniedException | IllegalStateException ex) {
            return new AccountClosureEligibility(false, ex.getMessage(), requiresPassword);
        }
    }

    @Transactional
    public void requestClosure(String password) {
        Context context = loadContext();
        String key = context.usuario().getId() + ":" + context.company().getId();
        sharedRateLimitService.enforce("account-closure-request", key, 3, Duration.ofHours(1));
        assertCanClose(context);
        if (context.credencial().isPasswordChanged()) {
            sharedRateLimitService.enforce("account-closure-password", key, 5, Duration.ofMinutes(15));
            if (password == null || !passwordEncoder.matches(password, context.credencial().getPassword())) {
                throw new IllegalArgumentException("La contraseña es incorrecta");
            }
        }
        String code = verificationCodeService.issue(context.usuario(), context.company(), CODE_PURPOSE);
        Map<String, Object> model = baseModel(context.usuario(), context.company());
        model.put("code", code);
        model.put("validityMinutes", codeValidityMinutes);
        Mail mail = emailService.createMail(context.usuario().getEmail(), "Código para cerrar tu cuenta", model);
        emailService.sendEmailWithRetry(mail, "email/account-close-code-template");
        auditLogService.log(context.company().getId(), "SOLICITAR_CIERRE_CUENTA", "Seguridad",
                "El usuario pidió el código para cerrar su cuenta");
    }

    @Transactional
    public void confirmClosure(String code) {
        Context context = loadContext();
        assertCanClose(context);
        verificationCodeService.verifyAndConsume(
                context.usuario().getId(), context.company().getId(), CODE_PURPOSE, code);

        LocalDateTime now = AppClock.now();
        String email = context.usuario().getEmail();
        CierreCuenta closure = new CierreCuenta();
        closure.setUsuario(context.usuario());
        closure.setCompany(context.company());
        closure.setEstado(EstadoCierreCuenta.CERRADA);
        closure.setCerradaAt(now);
        closure.setVenceAt(now.plusDays(graceDays));
        String reactivationToken = SecurityTokenUtils.generate();
        closure.setTokenHash(SecurityTokenUtils.hash(reactivationToken));

        if (context.empleado() != null) {
            Empleado empleado = context.empleado();
            empleado.setEstado(false);
            empleado.setTipoInactividad(TipoInactividad.BAJA);
            empleado.setEstadoModificadoPor(email);
            empleado.setFechaModificacionEstado(now);
            empleado.setUpdatedAt(now);
            empleadoRepository.save(empleado);
            closure.setEmpleadoId(empleado.getId());
        }
        if (context.apoderado() != null) {
            Apoderado apoderado = context.apoderado();
            apoderado.setEstado(false);
            apoderado.setTipoInactividad(TipoInactividad.BAJA);
            apoderado.setFechaSalida(AppClock.today());
            apoderado.setEstadoModificadoPor(email);
            apoderado.setFechaModificacionEstado(now);
            apoderadoRepository.save(apoderado);
            closure.setApoderadoId(apoderado.getId());
            petOwnershipService.syncPets(apoderado, TipoInactividad.BAJA);
        }
        cierreCuentaRepository.save(closure);
        companyMembershipService.syncLegacyCompanyField(context.usuario());

        passwordResetTokenRepository.deleteByUsuarioAndCompany(context.usuario(), context.company());
        sessionSecurityService.invalidateSessionsForCredential(context.credencial());

        auditLogService.log(context.company().getId(), "CERRAR_CUENTA", "Seguridad",
                "El usuario cerró su cuenta en esta empresa: " + displayName(context.usuario()) + " (" + email + ")");

        Map<String, Object> model = baseModel(context.usuario(), context.company());
        model.put("graceDays", graceDays);
        model.put("expiresOn", closure.getVenceAt().format(DATE));
        model.put("reactivateUrl", frontendUrl + EmailLinkUtils.withSlug(
                "/reactivate-account#token=" + reactivationToken, context.company().getSlug()));
        Mail mail = emailService.createMail(email, "Cerraste tu cuenta en " + model.get("companyName"), model);
        emailService.sendEmailWithRetry(mail, "email/account-closed-template");
    }

    @Transactional
    public void reactivate(String token) {
        if (token == null || token.isBlank()) {
            throw new ResourceNotFoundException("El enlace no es válido o ya venció");
        }
        CierreCuenta closure = cierreCuentaRepository.findByTokenHashForUpdate(SecurityTokenUtils.hash(token.trim()))
                .filter(c -> c.getEstado() == EstadoCierreCuenta.CERRADA)
                .filter(c -> c.getVenceAt().isAfter(AppClock.now()))
                .orElseThrow(() -> new ResourceNotFoundException("El enlace no es válido o ya venció"));
        Usuario usuario = closure.getUsuario();
        Company company = closure.getCompany();
        LocalDateTime now = AppClock.now();

        if (closure.getEmpleadoId() != null) {
            empleadoRepository.findById(closure.getEmpleadoId()).ifPresent(empleado -> {
                empleado.setEstado(true);
                empleado.setTipoInactividad(null);
                empleado.setEstadoModificadoPor(usuario.getEmail());
                empleado.setFechaModificacionEstado(now);
                empleado.setUpdatedAt(now);
                empleadoRepository.save(empleado);
            });
        }
        if (closure.getApoderadoId() != null) {
            apoderadoRepository.findById(closure.getApoderadoId()).ifPresent(apoderado -> {
                apoderado.setEstado(true);
                apoderado.setTipoInactividad(null);
                apoderado.setFechaSalida(null);
                apoderado.setEstadoModificadoPor(usuario.getEmail());
                apoderado.setFechaModificacionEstado(now);
                apoderadoRepository.save(apoderado);
                petOwnershipService.syncPets(apoderado, null);
            });
        }
        closure.setEstado(EstadoCierreCuenta.REACTIVADA);
        closure.setReactivadaAt(now);
        cierreCuentaRepository.save(closure);
        companyMembershipService.syncLegacyCompanyField(usuario);

        auditLogService.log(usuario.getEmail(), "USER", company.getId(), company.getName(), "REACTIVAR_CUENTA",
                "Seguridad", "El usuario reactivó su cuenta cerrada: " + displayName(usuario) + " (" + usuario.getEmail() + ")",
                null);

        Map<String, Object> model = baseModel(usuario, company);
        model.put("loginUrl", frontendUrl + EmailLinkUtils.withSlug("/login", company.getSlug()));
        Mail mail = emailService.createMail(usuario.getEmail(), "Reactivaste tu cuenta en " + model.get("companyName"), model);
        emailService.sendEmailWithRetry(mail, "email/account-reactivated-template");
    }

    @Transactional
    public int purgeExpired() {
        LocalDateTime now = AppClock.now();
        java.util.List<CierreCuenta> expired = cierreCuentaRepository
                .findTop100ByEstadoAndVenceAtBeforeOrderByVenceAtAsc(EstadoCierreCuenta.CERRADA, now);
        expired.forEach(closure -> purge(closure, now));
        return expired.size();
    }

    private void purge(CierreCuenta closure, LocalDateTime now) {
        Usuario usuario = closure.getUsuario();
        Company company = closure.getCompany();
        credencialRepository.findByUsuarioIdAndCompanyId(usuario.getId(), company.getId()).ifPresent(credencial -> {
            credencial.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            credencial.setPasswordChanged(false);
            credencial.setActivatedWithGoogle(false);
            credencial.setCredentialsVersion(credencial.getCredentialsVersion() + 1L);
            credencial.setUpdatedAt(now);
            credencialRepository.save(credencial);
        });
        sessionSecurityService.invalidateSessionsForCompany(usuario, company);
        passwordResetTokenRepository.deleteByUsuarioAndCompany(usuario, company);
        closure.setEstado(EstadoCierreCuenta.PURGADA);
        closure.setPurgadaAt(now);
        cierreCuentaRepository.save(closure);
        auditLogService.log("sistema", "SYSTEM", company.getId(), company.getName(), "PURGAR_CREDENCIALES_CIERRE",
                "Seguridad", "Venció el plazo de reactivación y se eliminaron las credenciales de " + displayName(usuario)
                        + " (" + usuario.getEmail() + ")", null);
    }

    private Context loadContext() {
        Integer userId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (SecurityUtils.isSuperAdmin() || companyId == null) {
            throw new AccessDeniedException("La cuenta de plataforma no se puede cerrar");
        }
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        UsuarioEmpresaCredencial credencial = credencialRepository.findByUsuarioIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));
        Empleado empleado = empleadoRepository.findByUserIdAndCompanyIdAndEstadoTrue(userId, companyId).orElse(null);
        Apoderado apoderado = apoderadoRepository.findByUserIdAndCompanyId(userId, companyId)
                .filter(a -> Boolean.TRUE.equals(a.getEstado())).orElse(null);
        return new Context(usuario, company, credencial, empleado, apoderado);
    }

    private void assertCanClose(Context context) {
        RolePurpose purpose = SecurityUtils.getCurrentRolePurpose();
        if (purpose != RolePurpose.COMPANY_ADMIN && purpose != RolePurpose.CLIENT_PORTAL) {
            throw new AccessDeniedException("Solo los administradores y los clientes pueden cerrar su cuenta. "
                    + "Tu salida de la clínica la gestiona un administrador.");
        }
        if (context.empleado() == null && context.apoderado() == null) {
            throw new IllegalStateException("Tu cuenta ya no tiene acceso activo en esta empresa");
        }
        Integer companyId = context.company().getId();
        if (context.empleado() != null) {
            if (!administratorProtection.isAdministrator(context.usuario(), companyId)) {
                throw new AccessDeniedException("Tienes un puesto en esta clínica. Tu salida la gestiona un administrador.");
            }
            administratorProtection.assertNotLastAdministrator(context.usuario(), companyId);
        }
        if (sesionCajaRepository.findAllByCompanyIdAndEstado(companyId, EstadoSesionCaja.ABIERTA).stream()
                .anyMatch(session -> openedBy(session, context.usuario()))) {
            throw new IllegalStateException("Tienes la caja abierta. Ciérrala antes de cerrar tu cuenta.");
        }
        LocalDateTime now = AppClock.now();
        if (context.empleado() != null && citaRepository.existsCitaVigenteByEmpleadoId(context.empleado().getId(), now)) {
            throw new IllegalStateException(
                    "Tienes citas programadas como profesional. Reasígnalas o cancélalas antes de cerrar tu cuenta.");
        }
        if (context.apoderado() != null && citaRepository.existsCitaVigenteByApoderadoId(context.apoderado().getId(), now)) {
            throw new IllegalStateException(
                    "Tienes citas programadas. Cancélalas o espera a que se realicen antes de cerrar tu cuenta.");
        }
        if (context.apoderado() != null) {
            java.math.BigDecimal deuda = citaRepository.saldoPendienteByApoderadoId(context.apoderado().getId());
            if (deuda != null && deuda.signum() > 0) {
                throw new IllegalStateException("Tienes una deuda pendiente de S/ " + deuda.setScale(2, java.math.RoundingMode.HALF_UP)
                        + " con la clínica. Págala antes de cerrar tu cuenta.");
            }
        }
    }

    private boolean openedBy(SesionCaja session, Usuario usuario) {
        if (session.getAbiertaPorUsuarioId() != null) {
            return session.getAbiertaPorUsuarioId().equals(usuario.getId());
        }
        return usuario.getEmail() != null && usuario.getEmail().equalsIgnoreCase(session.getAbiertaPor());
    }

    private Map<String, Object> baseModel(Usuario usuario, Company company) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", displayName(usuario));
        model.put("companyName", company != null && company.getName() != null ? company.getName() : defaultCompanyName);
        return model;
    }

    private String displayName(Usuario usuario) {
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }
}
