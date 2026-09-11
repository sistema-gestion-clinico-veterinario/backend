package veterinaria.vargasvet.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import veterinaria.vargasvet.dto.Mail;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    /** Reintentos para correos críticos (activación de cuenta, reset de contraseña): 1 intento inicial + 2 reintentos. */
    private static final int INTENTOS_CRITICOS = 3;

    /** Campo de instancia (no `static final`) para que los tests puedan acortarlo y no dormir segundos reales. */
    private long[] esperaEntreIntentosMs = {3_000, 8_000};

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AuditLogService auditLogService;

    @Value("${app.mail.from}")
    private String defaultFrom;

    public Mail createMail(String to, String subject, Map<String, Object> model) {
        Mail mail = new Mail();
        mail.setFrom(defaultFrom);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setModel(model);
        return mail;
    }

    /**
     * Envío estándar: un único intento. Devuelve si realmente se envió o no, para que quien
     * llama pueda decidir (por ejemplo, no marcar un recordatorio como "enviado" si falló).
     * No lanza excepción hacia quien llama: como corre en un hilo aparte (@Async), una excepción
     * nunca llegaría de vuelta de todas formas — por eso se resuelve como Future<Boolean> en su lugar.
     */
    @Async
    public CompletableFuture<Boolean> sendEmail(Mail mail, String templateName) {
        boolean enviado = intentarEnvio(mail, templateName);
        if (!enviado) {
            registrarFalloEnAuditoria(mail, 1);
        }
        return CompletableFuture.completedFuture(enviado);
    }

    /**
     * Envío para flujos críticos (activación de cuenta, reset de contraseña): reintenta hasta
     * {@value #INTENTOS_CRITICOS} veces SOLO si el intento anterior falló genuinamente (excepción
     * del proveedor de correo). Nunca reintenta después de un envío exitoso, así que no hay riesgo
     * de que el destinatario reciba el mismo correo duplicado — cada llamada a este método
     * corresponde a un único evento de negocio (una activación, un reset) y produce como máximo
     * un correo realmente entregado.
     */
    @Async
    public CompletableFuture<Boolean> sendEmailWithRetry(Mail mail, String templateName) {
        for (int intento = 1; intento <= INTENTOS_CRITICOS; intento++) {
            if (intentarEnvio(mail, templateName)) {
                return CompletableFuture.completedFuture(true);
            }
            boolean esUltimoIntento = intento == INTENTOS_CRITICOS;
            if (!esUltimoIntento) {
                log.warn("Reintentando envío de correo a {} (intento {} de {})", mail.getTo(), intento + 1, INTENTOS_CRITICOS);
                dormir(esperaEntreIntentosMs[intento - 1]);
            }
        }
        log.error("No se pudo enviar el correo crítico a {} tras {} intentos", mail.getTo(), INTENTOS_CRITICOS);
        registrarFalloEnAuditoria(mail, INTENTOS_CRITICOS);
        return CompletableFuture.completedFuture(false);
    }

    private boolean intentarEnvio(Mail mail, String templateName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            Context context = new Context();
            context.setVariables(mail.getModel());

            String html = templateEngine.process(templateName, context);
            helper.setTo(mail.getTo());
            helper.setText(html, true);
            helper.setSubject(mail.getSubject());
            helper.setFrom(mail.getFrom());

            mailSender.send(message);
            log.info("Correo enviado exitosamente a: {}", mail.getTo());
            return true;
        } catch (MessagingException | MailException e) {
            log.error("Error al enviar correo a {}: {}", mail.getTo(), e.getMessage());
            return false;
        }
    }

    private void registrarFalloEnAuditoria(Mail mail, int intentosRealizados) {
        try {
            auditLogService.log(
                    "ENVIO_CORREO_FALLIDO", "Correos",
                    "No se pudo enviar el correo \"" + mail.getSubject() + "\" a " + mail.getTo()
                            + " tras " + intentosRealizados + (intentosRealizados == 1 ? " intento" : " intentos")
                            + ". Verifique el estado del proveedor de correo.");
        } catch (Exception ex) {
            log.warn("No se pudo registrar en auditoría el fallo de envío de correo a {}: {}", mail.getTo(), ex.getMessage());
        }
    }

    private void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
