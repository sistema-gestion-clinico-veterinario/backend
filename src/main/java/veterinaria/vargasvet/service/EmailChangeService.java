package veterinaria.vargasvet.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.entity.UsuarioEmpresaCredencial;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.exception.MailDeliveryException;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.ApoderadoRepository;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.EmpleadoRepository;
import veterinaria.vargasvet.repository.MascotaRepository;
import veterinaria.vargasvet.repository.PasswordResetTokenRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioPorRolRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.CuentaPendiente;
import veterinaria.vargasvet.util.MailDelivery;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class EmailChangeService {

    public enum AdministrativeResult {
        CONFIRMATION_PENDING,
        PENDING_ACCOUNT_CORRECTED_WITH_INVITATION,
        PENDING_ACCOUNT_CORRECTED
    }

    private static final long DUPLICATE_WINDOW_MINUTES = 5;

    private static final String MAIL_FAILURE_MESSAGE =
            "No pudimos enviar el correo de confirmación. No se guardó ningún cambio; intenta de nuevo en unos minutos";

    private final UsuarioRepository usuarioRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SessionSecurityService sessionSecurityService;
    private final AuditLogService auditLogService;
    private final SharedRateLimitService sharedRateLimitService;
    private final UsuarioEmpresaCredencialRepository credencialRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipService companyMembershipService;
    private final UsuarioPorRolRepository usuarioPorRolRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ApoderadoRepository apoderadoRepository;
    private final MascotaRepository mascotaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${security.email-change-validity-minutes:30}")
    private long validityMinutes;

    /** El cambio iniciado por un administrador lo confirma la persona cuando revise su correo
     * nuevo, no en el momento: se le da el mismo plazo que a una invitación de activación. */
    @Value("${security.verification-token-validity-hours:24}")
    private long administrativeValidityHours;

    @Value("${app.url}")
    private String frontendUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

    @Value("${app.company.logo:}")
    private String defaultCompanyLogo;

    @Value("${app.company.email:}")
    private String defaultCompanyEmail;

    @Value("${app.company.phone:}")
    private String defaultCompanyPhone;

    @Value("${app.company.address:}")
    private String defaultCompanyAddress;

    @Value("${app.rate-limit.recovery-per-account-per-hour:3}")
    private int recoveryPerAccountPerHour;

    @Transactional
    public void requestChange(Integer usuarioId, RequestEmailChangeDTO dto) {
        // Por id, no por email: el correo ya no identifica de forma unica a la
        // sesion actual (puede repetirse entre usuarios distintos).
        sharedRateLimitService.enforce("email-change-account", String.valueOf(usuarioId),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!usuario.isActivo() || !usuario.isEmailVerified()) {
            throw new IllegalStateException("La cuenta no está habilitada para cambiar el correo");
        }
        // Verifica la credencial de la empresa activa de la sesión - un cambio de correo
        // (dato global) igual se confirma con la contraseña de la empresa desde la que se
        // está operando, no una "contraseña única" que ya no existe.
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        var credencial = (companyId == null
                ? credencialRepository.findByUsuarioIdAndCompanyIsNull(usuarioId)
                : credencialRepository.findByUsuarioIdAndCompanyId(usuarioId, companyId))
                .orElseThrow(() -> new IllegalStateException("No se encontró la credencial de esta empresa"));
        if (!credencial.isPasswordChanged()) {
            throw new IllegalArgumentException("Aún no creaste una contraseña propia (por ejemplo, activaste la cuenta con Google). "
                    + "Cierra sesión y usa «Olvidé mi contraseña» para crearla; después podrás cambiar tu correo.");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), credencial.getPassword())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        String newEmail = normalizeEmail(dto.getNewEmail());
        if (usuario.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("El nuevo correo debe ser diferente del correo actual");
        }
        if (emailEnUsoEnLaMismaEmpresa(usuario, newEmail)) {
            throw new IllegalArgumentException("El nuevo correo no está disponible");
        }

        // La empresa de LA SESION ACTIVA (no Usuario.company, esa cache legacy puede
        // estar vacia/desactualizada) - es la que se guarda en la solicitud para armar
        // el enlace de confirmacion con el slug correcto.
        Company company = companyId == null ? null : companyRepository.findById(companyId).orElse(null);

        replacePendingRequest(usuario);
        String oldToken = SecurityTokenUtils.generate();
        String newToken = SecurityTokenUtils.generate();
        LocalDateTime now = AppClock.now();

        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setCompany(company);
        request.setNewEmail(newEmail);
        request.setOldEmailTokenHash(SecurityTokenUtils.hash(oldToken));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash(newToken));
        request.setCreatedAt(now);
        request.setExpiresAt(now.plusMinutes(validityMinutes));
        emailChangeRequestRepository.save(request);

        CompletableFuture<Boolean> aCorreoActual =
                sendConfirmation(usuario.getEmail(), oldToken, "actual", usuario, company, newEmail, false);
        CompletableFuture<Boolean> aCorreoNuevo =
                sendConfirmation(newEmail, newToken, "nuevo", usuario, company, newEmail, false);
        if (MailDelivery.failed(aCorreoActual) | MailDelivery.failed(aCorreoNuevo)) {
            throw new MailDeliveryException(MAIL_FAILURE_MESSAGE);
        }
        auditLogService.log(company != null ? company.getId() : null, "SOLICITAR_CAMBIO_CORREO", "Seguridad",
                "El usuario inició un cambio de correo con doble confirmación");
    }

    /**
     * Cambio de correo iniciado por un administrador, para quien perdió el acceso a su correo
     * anterior. No se aplica en el acto: la identidad se verificó fuera del sistema, pero el
     * correo nuevo debe confirmarlo la persona, y al correo anterior se le avisa. Nunca sobre
     * la propia cuenta del administrador ni sobre cuentas administradoras (salvo plataforma).
     * Una cuenta que aún nadie activó no tiene a quién confirmar ni a quién avisar: se le corrige
     * el correo y se le envía una invitación nueva.
     */
    @Transactional
    public AdministrativeResult requestAdministrativeChange(Integer targetUserId, String rawNewEmail, String reason) {
        Integer actorId = SecurityUtils.getCurrentUserId();
        if (targetUserId != null && targetUserId.equals(actorId)) {
            throw new IllegalArgumentException(
                    "No puedes cambiar tu propio correo por esta vía; hazlo desde tu perfil o pide ayuda a otro administrador");
        }

        Usuario usuario = findManageableUser(targetUserId);
        Integer sessionCompanyId = SecurityUtils.getCurrentCompanyId();
        sharedRateLimitService.enforce("admin-email-change-target",
                (sessionCompanyId == null ? "plataforma" : String.valueOf(sessionCompanyId)) + ":" + targetUserId,
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));
        entityManager.lock(usuario, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(usuario);

        if (!SecurityUtils.isSuperAdmin() && isAdministratorAccount(usuario)) {
            throw new AccessDeniedException(
                    "Solo un administrador de plataforma puede cambiar el correo de una cuenta administradora");
        }
        boolean tieneContrasena = credencialRepository.findAllByUsuarioId(usuario.getId()).stream()
                .anyMatch(UsuarioEmpresaCredencial::isPasswordChanged);
        boolean pendiente = CuentaPendiente.es(usuario, tieneContrasena);
        if (!pendiente && (!usuario.isActivo() || !usuario.isEmailVerified())) {
            throw new IllegalStateException("La cuenta no está habilitada para cambiar el correo");
        }

        String newEmail = normalizeEmail(rawNewEmail);
        if (isRecentDuplicate(usuario, newEmail, pendiente)) {
            return pendiente ? AdministrativeResult.PENDING_ACCOUNT_CORRECTED_WITH_INVITATION
                    : AdministrativeResult.CONFIRMATION_PENDING;
        }
        if (usuario.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("El nuevo correo debe ser diferente del correo actual");
        }
        if (emailEnUsoEnLaMismaEmpresa(usuario, newEmail)) {
            throw new IllegalArgumentException("El nuevo correo no está disponible");
        }

        Company company = resolveCompanyForLink(usuario);
        Integer auditCompanyId = company != null ? company.getId() : null;
        String note = reason == null || reason.isBlank() ? "" : ". Nota: " + reason.trim();
        if (pendiente) {
            return correctPendingAccount(usuario, newEmail, company, auditCompanyId, note);
        }

        replacePendingRequest(usuario);
        LocalDateTime now = AppClock.now();
        String newToken = SecurityTokenUtils.generate();
        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setCompany(company);
        request.setNewEmail(newEmail);
        // El correo anterior se da por autorizado por el administrador: su token solo sirve para
        // que quien conserva ese correo pueda cancelar la solicitud ("no fui yo").
        String cancelToken = SecurityTokenUtils.generate();
        request.setOldEmailTokenHash(SecurityTokenUtils.hash(cancelToken));
        request.setOldEmailConfirmedAt(now);
        request.setNewEmailTokenHash(SecurityTokenUtils.hash(newToken));
        request.setCreatedAt(now);
        request.setExpiresAt(now.plusHours(administrativeValidityHours));
        emailChangeRequestRepository.save(request);

        if (MailDelivery.failed(sendConfirmation(newEmail, newToken, "nuevo", usuario, company, newEmail, true))) {
            throw new MailDeliveryException(MAIL_FAILURE_MESSAGE);
        }
        sendAdministrativeNotice(usuario.getEmail(), usuario, company, newEmail, cancelToken);

        auditLogService.log(auditCompanyId, "SOLICITAR_CAMBIO_CORREO_ADMINISTRATIVO", "Seguridad",
                "Un administrador solicitó cambiar el correo de " + fullName(usuario) + " (" + usuario.getEmail()
                        + ") a " + newEmail + note);
        return AdministrativeResult.CONFIRMATION_PENDING;
    }

    /** El mismo pedido repetido a los pocos minutos (doble clic, o dos administradores) no genera otro
     * enlace ni otro correo: el que ya se envió sigue siendo el válido. */
    private boolean isRecentDuplicate(Usuario usuario, String newEmail, boolean pendiente) {
        LocalDateTime now = AppClock.now();
        LocalDateTime windowStart = now.minusMinutes(DUPLICATE_WINDOW_MINUTES);
        if (pendiente) {
            LocalDateTime expiresAt = usuario.getVerificationTokenExpiresAt();
            return usuario.getEmail().equalsIgnoreCase(newEmail) && expiresAt != null
                    && expiresAt.minusHours(administrativeValidityHours).isAfter(windowStart);
        }
        return emailChangeRequestRepository.findByUsuario(usuario)
                .filter(request -> request.getNewEmail().equalsIgnoreCase(newEmail)
                        && request.getCreatedAt().isAfter(windowStart)
                        && request.getExpiresAt().isAfter(now))
                .isPresent();
    }

    private AdministrativeResult correctPendingAccount(Usuario usuario, String newEmail, Company company,
                                                       Integer auditCompanyId, String note) {
        String oldEmail = usuario.getEmail();
        syncUsernameWithEmail(usuario, oldEmail, newEmail);
        usuario.setEmail(newEmail);
        replacePendingRequest(usuario);

        boolean invite = shouldInvite(usuario, company);
        String token = null;
        if (invite) {
            token = SecurityTokenUtils.generate();
            usuario.setVerificationToken(SecurityTokenUtils.hash(token));
            usuario.setVerificationTokenExpiresAt(AppClock.now().plusHours(administrativeValidityHours));
        } else {
            usuario.setVerificationToken(null);
            usuario.setVerificationTokenExpiresAt(null);
        }
        usuarioRepository.save(usuario);

        if (invite && MailDelivery.failed(sendInvitation(usuario, token, company))) {
            throw new MailDeliveryException(MAIL_FAILURE_MESSAGE);
        }
        auditLogService.log(auditCompanyId, "CORREGIR_CORREO_CUENTA_PENDIENTE", "Seguridad",
                "Se corrigió el correo de la cuenta pendiente de " + fullName(usuario) + ": " + oldEmail
                        + " → " + newEmail + (invite ? ". Se envió una invitación nueva" : ". Aún no se le envía invitación") + note);
        return invite ? AdministrativeResult.PENDING_ACCOUNT_CORRECTED_WITH_INVITATION
                : AdministrativeResult.PENDING_ACCOUNT_CORRECTED;
    }

    /** Un cliente sin mascotas todavía no recibe invitación (se le envía al registrar la primera);
     * cualquier otra cuenta pendiente sí. */
    private boolean shouldInvite(Usuario usuario, Company company) {
        if (company == null) {
            return true;
        }
        Integer companyId = company.getId();
        if (empleadoRepository.existsByUserIdAndCompanyId(usuario.getId(), companyId)) {
            return true;
        }
        return apoderadoRepository.findByUserIdAndCompanyId(usuario.getId(), companyId)
                .map(apoderado -> mascotaRepository.existsByApoderadoIdAndActivoTrue(apoderado.getId()))
                .orElse(true);
    }

    private Usuario findManageableUser(Integer userId) {
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (SecurityUtils.isSuperAdmin()) {
            return usuario;
        }
        // Cada empresa tiene sus propias cuentas (el mismo correo en dos empresas son cuentas
        // distintas): el administrador gestiona las cuentas de su empresa y nada más.
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (companyId == null || !companyMembershipService.hasActiveMembership(userId, companyId)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }
        return usuario;
    }

    private boolean isAdministratorAccount(Usuario usuario) {
        return usuarioPorRolRepository.findByUsuarioId(usuario.getId()).stream()
                .map(assignment -> assignment.getRol())
                .anyMatch(role -> role != null && role.isActivo()
                        && (role.getPurpose() == RolePurpose.COMPANY_ADMIN
                        || role.getPurpose() == RolePurpose.PLATFORM_ADMIN));
    }

    private Company resolveCompanyForLink(Usuario usuario) {
        Integer sessionCompanyId = SecurityUtils.getCurrentCompanyId();
        if (sessionCompanyId != null) {
            return companyRepository.findById(sessionCompanyId).orElse(null);
        }
        if (usuario.getCompany() != null) {
            return usuario.getCompany();
        }
        return companyMembershipService.getActiveCompanyIds(usuario).stream().findFirst()
                .flatMap(companyRepository::findById).orElse(null);
    }

    /** Hibernate ejecuta el INSERT antes que el DELETE pendiente: sin el flush, pedir otro cambio
     * mientras hay uno vigente viola el índice único de usuario. */
    private void replacePendingRequest(Usuario usuario) {
        emailChangeRequestRepository.deleteByUsuario(usuario);
        emailChangeRequestRepository.flush();
    }

    /**
     * "No fui yo": quien conserva el correo actual cancela la solicitud pendiente. Se identifica con
     * el mismo token del correo actual (en el cambio administrativo, el que lleva el aviso).
     * Borra la solicitud, por lo que ni el correo nuevo ni el actual pueden completarla, y deja
     * constancia en la auditoría para que los administradores la vean.
     */
    @Transactional
    public void cancelRequest(String rawToken) {
        EmailChangeRequest request = emailChangeRequestRepository
                .findByOldTokenForUpdate(SecurityTokenUtils.hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("El enlace es inválido o ya fue utilizado"));
        Usuario usuario = request.getUsuario();
        Company company = request.getCompany();
        emailChangeRequestRepository.delete(request);
        auditLogService.log(usuario.getEmail(), "USER",
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                "CANCELAR_CAMBIO_CORREO", "Seguridad",
                "La persona que conserva el correo actual canceló la solicitud de cambio de correo de "
                        + fullName(usuario) + " (" + usuario.getEmail() + ") a " + request.getNewEmail()
                        + ", que no reconocía", null);
    }

    @Transactional
    public boolean confirmCurrentEmail(String rawToken) {
        EmailChangeRequest request = emailChangeRequestRepository
                .findByOldTokenForUpdate(SecurityTokenUtils.hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("El enlace es inválido o ya fue utilizado"));
        validateNotExpired(request);
        request.setOldEmailConfirmedAt(AppClock.now());
        return completeWhenBothConfirmed(request);
    }

    @Transactional
    public boolean confirmNewEmail(String rawToken) {
        EmailChangeRequest request = emailChangeRequestRepository
                .findByNewTokenForUpdate(SecurityTokenUtils.hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("El enlace es inválido o ya fue utilizado"));
        validateNotExpired(request);
        request.setNewEmailConfirmedAt(AppClock.now());
        return completeWhenBothConfirmed(request);
    }

    private boolean completeWhenBothConfirmed(EmailChangeRequest request) {
        if (request.getOldEmailConfirmedAt() == null || request.getNewEmailConfirmedAt() == null) {
            emailChangeRequestRepository.save(request);
            return false;
        }

        Usuario usuario = request.getUsuario();
        Company company = request.getCompany();
        if (company != null && !companyMembershipService.hasActiveMembership(usuario.getId(), company.getId())) {
            throw new IllegalStateException(
                    "Esta cuenta ya no tiene acceso activo a la empresa, por lo que el cambio de correo no se aplicó");
        }
        if (emailEnUsoEnLaMismaEmpresa(usuario, request.getNewEmail())) {
            throw new IllegalStateException("El correo nuevo dejó de estar disponible");
        }

        String oldEmail = usuario.getEmail();
        syncUsernameWithEmail(usuario, oldEmail, request.getNewEmail());
        usuario.setEmail(request.getNewEmail());
        usuario.setEmailVerified(true);
        sessionSecurityService.invalidateAllSessions(usuario);
        passwordResetTokenRepository.deleteByUsuario(usuario);
        emailChangeRequestRepository.delete(request);

        auditLogService.log(usuario.getEmail(), "USER",
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                "CONFIRMAR_CAMBIO_CORREO", "Seguridad",
                "Se completó el cambio de correo de " + fullName(usuario) + ": " + oldEmail + " → " + usuario.getEmail()
                        + ". Se cerraron todas sus sesiones y se anularon sus enlaces de restablecimiento pendientes", null);
        sendCompletedNotice(oldEmail, usuario, company);
        return true;
    }

    /** El login acepta username O correo en el mismo campo. Si la persona escribió su
     * correo como username al registrarse (muy comun), cambiar solo Usuario.email
     * dejaba el correo VIEJO funcionando para siempre como credencial de acceso, via
     * el campo username que este flujo nunca tocaba - justo el escenario de riesgo
     * que se quiere evitar al cambiar de correo (perdida de acceso al correo viejo).
     * Se sincroniza SOLO si coincidian, y solo si el nuevo correo no choca con el
     * username de otra cuenta de esta misma empresa. */
    private void syncUsernameWithEmail(Usuario usuario, String oldEmail, String newEmail) {
        if (usuario.getUsername().equalsIgnoreCase(oldEmail) && !usernameEnUsoEnLaMismaEmpresa(usuario, newEmail)) {
            usuario.setUsername(newEmail);
        }
    }

    /** El correo se valida como duplicado SOLO dentro de la misma empresa del usuario -
     * aislamiento total entre empresas, nunca se cruza contra las demas. */
    private boolean emailEnUsoEnLaMismaEmpresa(Usuario usuario, String email) {
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        boolean porEmpresaGuardada = companyId == null
                ? usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull(email)
                : usuarioRepository.existsByEmailIgnoreCaseAndCompanyId(email, companyId);
        // Quien pertenece a varias empresas no tiene empresa guardada: se revisa por membresías.
        return porEmpresaGuardada || companyMembershipService.isEmailTakenInUserCompanies(usuario, email);
    }

    private boolean usernameEnUsoEnLaMismaEmpresa(Usuario usuario, String username) {
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        boolean porEmpresaGuardada = companyId == null
                ? usuarioRepository.existsByUsernameIgnoreCaseAndCompanyIsNull(username)
                : usuarioRepository.existsByUsernameIgnoreCaseAndCompanyId(username, companyId);
        return porEmpresaGuardada || companyMembershipService.isUsernameTakenInUserCompanies(usuario, username);
    }

    private void validateNotExpired(EmailChangeRequest request) {
        if (request.getExpiresAt().isBefore(AppClock.now())) {
            emailChangeRequestRepository.delete(request);
            throw new IllegalArgumentException("El enlace expiró; solicite nuevamente el cambio");
        }
    }

    private CompletableFuture<Boolean> sendConfirmation(String destination, String token, String confirmationType,
                                                        Usuario usuario, Company company, String newEmail,
                                                        boolean administrative) {
        Map<String, Object> model = baseModel(usuario, company);
        model.put("newEmail", newEmail);
        model.put("confirmationType", confirmationType);
        model.put("administrative", administrative);
        String slug = company != null ? company.getSlug() : null;
        model.put("confirmationUrl", frontendUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                "/confirm-email-change#type=" + confirmationType + "&token=" + token, slug));
        if ("actual".equals(confirmationType)) {
            model.put("cancelUrl", cancelUrl(token, slug));
        }
        Mail mail = emailService.createMail(destination,
                "Confirmación de cambio de correo", model);
        return emailService.sendEmailWithRetry(mail, "email/email-change-confirmation-template");
    }

    private CompletableFuture<Boolean> sendInvitation(Usuario usuario, String token, Company company) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", fullName(usuario));
        model.put("validityHours", administrativeValidityHours);
        model.put("companyName", company != null && company.getName() != null ? company.getName() : defaultCompanyName);
        model.put("companyLogo", company != null && company.getLogoUrl() != null ? company.getLogoUrl() : defaultCompanyLogo);
        model.put("companyEmail", company != null && company.getEmail() != null ? company.getEmail() : defaultCompanyEmail);
        model.put("companyPhone", company != null && company.getPhone() != null ? company.getPhone() : defaultCompanyPhone);
        model.put("companyAddress", company != null && company.getAddress() != null ? company.getAddress() : defaultCompanyAddress);
        model.put("verificationLink", frontendUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                "/auth/verify#token=" + token, company != null ? company.getSlug() : null));
        Mail mail = emailService.createMail(usuario.getEmail(),
                "Activa tu cuenta en " + model.get("companyName"), model);
        return emailService.sendEmailWithRetry(mail, "email/welcome-template");
    }

    private String cancelUrl(String token, String slug) {
        return frontendUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                "/confirm-email-change#type=cancelar&token=" + token, slug);
    }

    private void sendAdministrativeNotice(String oldEmail, Usuario usuario, Company company, String newEmail,
                                          String cancelToken) {
        Map<String, Object> model = baseModel(usuario, company);
        model.put("newEmail", newEmail);
        model.put("validityHours", administrativeValidityHours);
        model.put("cancelUrl", cancelUrl(cancelToken, company != null ? company.getSlug() : null));
        Mail mail = emailService.createMail(oldEmail,
                "Se solicitó cambiar tu correo de acceso", model);
        emailService.sendEmailWithRetry(mail, "email/email-change-admin-notice-template");
    }

    private void sendCompletedNotice(String oldEmail, Usuario usuario, Company company) {
        Map<String, Object> model = baseModel(usuario, company);
        model.put("newEmail", usuario.getEmail());
        Mail mail = emailService.createMail(oldEmail,
                "Tu correo de acceso fue actualizado", model);
        emailService.sendEmailWithRetry(mail, "email/email-change-completed-template");
    }

    private Map<String, Object> baseModel(Usuario usuario, Company company) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", fullName(usuario));
        model.put("companyName", company != null ? company.getName() : defaultCompanyName);
        return model;
    }

    private String fullName(Usuario usuario) {
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
