package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import veterinaria.vargasvet.domain.entity.EmailChangeRequest;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.dto.request.RequestEmailChangeDTO;
import veterinaria.vargasvet.exception.ResourceNotFoundException;
import veterinaria.vargasvet.repository.EmailChangeRequestRepository;
import veterinaria.vargasvet.repository.UsuarioRepository;
import veterinaria.vargasvet.security.SecurityTokenUtils;
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

    @Value("${security.email-change-validity-minutes:30}")
    private long validityMinutes;

    @Value("${app.url}")
    private String frontendUrl;

    @Value("${app.company.name}")
    private String defaultCompanyName;

    @Value("${app.rate-limit.recovery-per-account-per-hour:3}")
    private int recoveryPerAccountPerHour;

    @Transactional
    public void requestChange(String authenticatedEmail, RequestEmailChangeDTO dto) {
        sharedRateLimitService.enforce("email-change-account", normalizeEmail(authenticatedEmail),
                recoveryPerAccountPerHour, java.time.Duration.ofHours(1));
        Usuario usuario = usuarioRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!usuario.isActivo() || !usuario.isEmailVerified()) {
            throw new IllegalStateException("La cuenta no está habilitada para cambiar el correo");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        String newEmail = normalizeEmail(dto.getNewEmail());
        if (usuario.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("El nuevo correo debe ser diferente del correo actual");
        }
        if (usuarioRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("El nuevo correo no está disponible");
        }

        emailChangeRequestRepository.deleteByUsuario(usuario);
        String oldToken = SecurityTokenUtils.generate();
        String newToken = SecurityTokenUtils.generate();
        LocalDateTime now = AppClock.now();

        EmailChangeRequest request = new EmailChangeRequest();
        request.setUsuario(usuario);
        request.setNewEmail(newEmail);
        request.setOldEmailTokenHash(SecurityTokenUtils.hash(oldToken));
        request.setNewEmailTokenHash(SecurityTokenUtils.hash(newToken));
        request.setCreatedAt(now);
        request.setExpiresAt(now.plusMinutes(validityMinutes));
        emailChangeRequestRepository.save(request);

        sendConfirmation(usuario.getEmail(), oldToken, "actual", usuario, newEmail);
        sendConfirmation(newEmail, newToken, "nuevo", usuario, newEmail);
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

        if (usuarioRepository.existsByEmail(request.getNewEmail())) {
            throw new IllegalStateException("El correo nuevo dejó de estar disponible");
        }

        Usuario usuario = request.getUsuario();
        String oldEmail = usuario.getEmail();
        usuario.setEmail(request.getNewEmail());
        usuario.setEmailVerified(true);
        sessionSecurityService.invalidateAllSessions(usuario);
        emailChangeRequestRepository.delete(request);

        auditLogService.log(usuario.getEmail(), "USER",
                usuario.getCompany() != null ? usuario.getCompany().getId() : null,
                usuario.getCompany() != null ? usuario.getCompany().getName() : null,
                "CONFIRMAR_CAMBIO_CORREO", "Seguridad",
                "Se completó un cambio de correo y se invalidaron todas las sesiones", null);
        sendCompletedNotice(oldEmail, usuario);
        return true;
    }

    private void validateNotExpired(EmailChangeRequest request) {
        if (request.getExpiresAt().isBefore(AppClock.now())) {
            emailChangeRequestRepository.delete(request);
            throw new IllegalArgumentException("El enlace expiró; solicite nuevamente el cambio");
        }
    }

    private void sendConfirmation(String destination, String token, String confirmationType,
                                  Usuario usuario, String newEmail) {
        Map<String, Object> model = baseModel(usuario);
        model.put("newEmail", newEmail);
        model.put("confirmationType", confirmationType);
        model.put("confirmationUrl", frontendUrl + "/confirm-email-change#type="
                + confirmationType + "&token=" + token);
        Mail mail = emailService.createMail(destination,
                "Confirmación de cambio de correo", model);
        emailService.sendEmail(mail, "email/email-change-confirmation-template");
    }

    private void sendCompletedNotice(String oldEmail, Usuario usuario) {
        Map<String, Object> model = baseModel(usuario);
        model.put("newEmail", usuario.getEmail());
        Mail mail = emailService.createMail(oldEmail,
                "Tu correo de acceso fue actualizado", model);
        emailService.sendEmail(mail, "email/email-change-completed-template");
    }

    private Map<String, Object> baseModel(Usuario usuario) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim());
        model.put("companyName", usuario.getCompany() != null
                ? usuario.getCompany().getName() : defaultCompanyName);
        return model;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
