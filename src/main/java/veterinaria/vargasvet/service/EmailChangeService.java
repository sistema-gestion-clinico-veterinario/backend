package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.CompanyRepository;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.UsuarioEmpresaCredencialRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
import veterinaria.vargasvet.security.SecurityUtils;
import veterinaria.vargasvet.security.SharedRateLimitService;
import veterinaria.vargasvet.util.AppClock;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private final UsuarioRepository usuarioRepository;
    private final EmailChangeRequestRepository emailChangeRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SessionSecurityService sessionSecurityService;
    private final AuditLogService auditLogService;
    private final SharedRateLimitService sharedRateLimitService;
    private final UsuarioEmpresaCredencialRepository credencialRepository;
    private final CompanyRepository companyRepository;

    @Value("${security.email-change-validity-minutes:30}")
    private long validityMinutes;

    @Value("${app.url}")
    private String frontendUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

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

        emailChangeRequestRepository.deleteByUsuario(usuario);
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

        sendConfirmation(usuario.getEmail(), oldToken, "actual", usuario, company, newEmail);
        sendConfirmation(newEmail, newToken, "nuevo", usuario, company, newEmail);
        auditLogService.log("SOLICITAR_CAMBIO_CORREO", "Seguridad",
                "El usuario inició un cambio de correo con doble confirmación");
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
        if (emailEnUsoEnLaMismaEmpresa(usuario, request.getNewEmail())) {
            throw new IllegalStateException("El correo nuevo dejó de estar disponible");
        }

        String oldEmail = usuario.getEmail();
        // El login acepta username O correo en el mismo campo. Si la persona escribió su
        // correo como username al registrarse (muy comun), cambiar solo Usuario.email
        // dejaba el correo VIEJO funcionando para siempre como credencial de acceso, via
        // el campo username que este flujo nunca tocaba - justo el escenario de riesgo
        // que se quiere evitar al cambiar de correo (perdida de acceso al correo viejo).
        // Se sincroniza SOLO si coincidian, y solo si el nuevo correo no choca con el
        // username de otra cuenta de esta misma empresa.
        if (usuario.getUsername().equalsIgnoreCase(oldEmail) && !usernameEnUsoEnLaMismaEmpresa(usuario, request.getNewEmail())) {
            usuario.setUsername(request.getNewEmail());
        }
        usuario.setEmail(request.getNewEmail());
        usuario.setEmailVerified(true);
        sessionSecurityService.invalidateAllSessions(usuario);
        emailChangeRequestRepository.delete(request);

        Company company = request.getCompany();
        auditLogService.log(usuario.getEmail(), "USER",
                company != null ? company.getId() : null,
                company != null ? company.getName() : null,
                "CONFIRMAR_CAMBIO_CORREO", "Seguridad",
                "Se completó un cambio de correo y se invalidaron todas las sesiones", null);
        sendCompletedNotice(oldEmail, usuario, company);
        return true;
    }

    /** El correo se valida como duplicado SOLO dentro de la misma empresa del usuario -
     * aislamiento total entre empresas, nunca se cruza contra las demas. */
    private boolean emailEnUsoEnLaMismaEmpresa(Usuario usuario, String email) {
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        return companyId == null
                ? usuarioRepository.existsByEmailIgnoreCaseAndCompanyIsNull(email)
                : usuarioRepository.existsByEmailIgnoreCaseAndCompanyId(email, companyId);
    }

    private boolean usernameEnUsoEnLaMismaEmpresa(Usuario usuario, String username) {
        Integer companyId = usuario.getCompany() != null ? usuario.getCompany().getId() : null;
        return companyId == null
                ? usuarioRepository.existsByUsernameIgnoreCaseAndCompanyIsNull(username)
                : usuarioRepository.existsByUsernameIgnoreCaseAndCompanyId(username, companyId);
    }

    private void validateNotExpired(EmailChangeRequest request) {
        if (request.getExpiresAt().isBefore(AppClock.now())) {
            emailChangeRequestRepository.delete(request);
            throw new IllegalArgumentException("El enlace expiró; solicite nuevamente el cambio");
        }
    }

    private void sendConfirmation(String destination, String token, String confirmationType,
                                  Usuario usuario, Company company, String newEmail) {
        Map<String, Object> model = baseModel(usuario, company);
        model.put("newEmail", newEmail);
        model.put("confirmationType", confirmationType);
        String slug = company != null ? company.getSlug() : null;
        model.put("confirmationUrl", frontendUrl + veterinaria.vargasvet.util.EmailLinkUtils.withSlug(
                "/confirm-email-change#type=" + confirmationType + "&token=" + token, slug));
        Mail mail = emailService.createMail(destination,
                "Confirmación de cambio de correo", model);
        emailService.sendEmail(mail, "email/email-change-confirmation-template");
    }

    private void sendCompletedNotice(String oldEmail, Usuario usuario, Company company) {
        Map<String, Object> model = baseModel(usuario, company);
        model.put("newEmail", usuario.getEmail());
        Mail mail = emailService.createMail(oldEmail,
                "Tu correo de acceso fue actualizado", model);
        emailService.sendEmail(mail, "email/email-change-completed-template");
    }

    private Map<String, Object> baseModel(Usuario usuario, Company company) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim());
        model.put("companyName", company != null ? company.getName() : defaultCompanyName);
        return model;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
