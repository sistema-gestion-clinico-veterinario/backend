package veterinaria.vargasvet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.EntregaAvisoPrivacidad;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.domain.enums.EstadoEntregaAviso;
import veterinaria.vargasvet.dto.Mail;
import veterinaria.vargasvet.repository.EntregaAvisoPrivacidadRepository;
import veterinaria.vargasvet.util.AppClock;
import veterinaria.vargasvet.util.EmailLinkUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AvisoPrivacidadEntregaService {

    private static final String CANAL_EMAIL = "EMAIL";

    private final EntregaAvisoPrivacidadRepository entregaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    @Value("${app.url}")
    private String appUrl;

    /** Programa una sola entrega por persona y versión. La salida SMTP ocurre únicamente después del commit. */
    @Transactional
    public void programarCorreo(Usuario usuario, AvisoPrivacidad aviso) {
        if (usuario == null || aviso == null || usuario.getEmail() == null || usuario.getEmail().isBlank()
                || entregaRepository.existsByUsuarioIdAndAvisoIdAndCanal(usuario.getId(), aviso.getId(), CANAL_EMAIL)) {
            return;
        }
        EntregaAvisoPrivacidad entrega = new EntregaAvisoPrivacidad();
        entrega.setCompany(aviso.getCompany());
        entrega.setUsuario(usuario);
        entrega.setAviso(aviso);
        entrega.setCanal(CANAL_EMAIL);
        entrega.setDestino(usuario.getEmail().strip().toLowerCase());
        entrega.setEstado(EstadoEntregaAviso.PENDIENTE);
        entrega.setSolicitadoEn(AppClock.now());
        try {
            entregaRepository.saveAndFlush(entrega);
            eventPublisher.publishEvent(new AvisoPrivacidadEntregaSolicitada(entrega.getId()));
        } catch (DataIntegrityViolationException ignored) {
            // Dos altas o roles concurrentes nunca deben producir dos correos de la misma versión.
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarDespuesDelCommit(AvisoPrivacidadEntregaSolicitada evento) {
        EntregaAvisoPrivacidad entrega = entregaRepository.findOneById(evento.entregaId()).orElse(null);
        if (entrega == null || entrega.getEstado() != EstadoEntregaAviso.PENDIENTE) return;

        AvisoPrivacidad aviso = entrega.getAviso();
        String slug = entrega.getCompany().getSlug();
        String enlace = appUrl + EmailLinkUtils.withSlug("/privacidad", slug)
                + "?audiencia=" + aviso.getAudiencia().name() + "&version=" + aviso.getVersion();
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", entrega.getUsuario().getNombre());
        model.put("companyName", entrega.getCompany().getName());
        model.put("companyLogo", entrega.getCompany().getLogoUrl());
        model.put("avisoVersion", aviso.getVersion());
        model.put("vigenteDesde", aviso.getVigenteDesde());
        model.put("avisoLink", enlace);
        model.put("esActualizacion", aviso.getVersion() > 1);

        Mail mail = emailService.createMail(entrega.getDestino(),
                (aviso.getVersion() > 1 ? "Actualización del aviso de privacidad - " : "Aviso de privacidad - ")
                        + entrega.getCompany().getName(), model);
        emailService.sendEmail(mail, "email/privacy-notice-template")
                .whenComplete((enviado, error) -> actualizarResultado(evento.entregaId(),
                        error == null && Boolean.TRUE.equals(enviado)));
    }

    @Transactional
    public void actualizarResultado(Long entregaId, boolean enviado) {
        entregaRepository.findById(entregaId).ifPresent(entrega -> {
            entrega.setEstado(enviado ? EstadoEntregaAviso.ENVIADO : EstadoEntregaAviso.FALLIDO);
            entrega.setEnviadoEn(enviado ? AppClock.now() : null);
            entrega.setDetalle(enviado ? null : "El proveedor de correo no confirmó el envío");
            entregaRepository.save(entrega);
        });
    }
}
