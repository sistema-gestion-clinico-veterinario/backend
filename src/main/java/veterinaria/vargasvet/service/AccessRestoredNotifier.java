package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.util.EmailLinkUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRestoredNotifier {

    private static final String TEMPLATE = "email/access-restored-template";

    private final EmailService emailService;

    @Value("${app.url}")
    private String frontendUrl;

    /** Avisa a la persona que le devolvieron el acceso. Solo si su cuenta ya está activada y una vez confirmada la
     * transacción; si el correo falla, la reactivación ya quedó hecha. */
    public void send(Usuario usuario, Company company) {
        if (usuario == null || company == null || !usuario.isEmailVerified() || usuario.getEmail() == null) {
            return;
        }
        Runnable sending = () -> {
            try {
                Map<String, Object> model = new HashMap<>();
                model.put("nombre", fullName(usuario));
                model.put("companyName", company.getName());
                model.put("loginUrl", frontendUrl + EmailLinkUtils.withSlug("/login", company.getSlug()));
                Mail mail = emailService.createMail(usuario.getEmail(),
                        "Tu acceso a " + company.getName() + " fue restablecido", model);
                emailService.sendEmailWithRetry(mail, TEMPLATE);
            } catch (Exception e) {
                log.warn("No se pudo preparar el aviso de acceso restablecido para {}: {}", usuario.getEmail(), e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sending.run();
                }
            });
        } else {
            sending.run();
        }
    }

    private String fullName(Usuario usuario) {
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }
}
