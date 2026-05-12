package veterinaria.vargasvet.shared;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import veterinaria.vargasvet.shared.Mail;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String defaultFrom;

    public Mail createMail(String to, String subject, Map<String, Object> model) {
        Mail mail = new Mail();
        mail.setFrom(defaultFrom);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setModel(model);
        return mail;
    }

    public void sendEmail(Mail mail, String templateName) {
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
        } catch (MessagingException e) {
            log.error("Error al enviar correo a {}: {}", mail.getTo(), e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo");
        }
    }
}
