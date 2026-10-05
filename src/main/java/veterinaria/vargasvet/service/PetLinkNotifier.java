package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetLinkNotifier {

    private static final String TEMPLATE = "email/vinculo-mascota-template";

    private final EmailService emailService;
    private final OwnerContactPolicy ownerContactPolicy;

    /** Avisa al propietario principal que se vinculó, modificó o revocó a otra persona. Va después de confirmada la
     * transacción y solo a un correo verificado; si falla, el cambio ya quedó hecho. */
    public void avisarAlPrincipal(Mascota mascota, Apoderado persona, String accion, String detalle) {
        Apoderado principal = mascota.getApoderado();
        if (principal == null || principal.getUser() == null || persona == null
                || !Boolean.TRUE.equals(principal.getEstado())
                || principal.getId().equals(persona.getId())
                || !ownerContactPolicy.puedeRecibirCorreo(principal.getUser())) {
            return;
        }
        Usuario usuario = principal.getUser();
        Company company = principal.getCompany();
        Runnable envio = () -> {
            try {
                Map<String, Object> model = new HashMap<>();
                model.put("nombre", nombre(usuario));
                model.put("companyName", company != null ? company.getName() : "su veterinaria");
                model.put("accion", accion);
                model.put("persona", persona.getUser() != null ? nombre(persona.getUser()) : "una persona");
                model.put("mascota", mascota.getNombreCompleto());
                model.put("detalle", detalle);
                Mail mail = emailService.createMail(usuario.getEmail(),
                        "Cambio en las personas vinculadas a " + mascota.getNombreCompleto(), model);
                emailService.sendEmailWithRetry(mail, TEMPLATE);
            } catch (Exception e) {
                log.warn("No se pudo avisar al propietario {} del cambio de vínculo: {}", usuario.getId(), e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private String nombre(Usuario usuario) {
        return ((usuario.getNombre() == null ? "" : usuario.getNombre()) + " "
                + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    }
}
