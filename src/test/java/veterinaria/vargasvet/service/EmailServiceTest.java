package veterinaria.vargasvet.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import veterinaria.vargasvet.dto.Mail;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock SpringTemplateEngine templateEngine;
    @Mock AuditLogService auditLogService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine, auditLogService);
        ReflectionTestUtils.setField(emailService, "defaultFrom", "no-reply@vargasvet.test");
        // Evita que el test real duerma segundos entre reintentos.
        ReflectionTestUtils.setField(emailService, "esperaEntreIntentosMs", new long[]{0, 0});
        lenient().when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        lenient().when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");
    }

    private Mail mail() {
        return new Mail("no-reply@vargasvet.test", "cliente@example.com", "Asunto de prueba", Map.of("nombre", "Ana"));
    }

    @Test
    void sendEmail_devuelveTrueYNoAuditaNadaCuandoElEnvioEsExitoso() {
        boolean resultado = emailService.sendEmail(mail(), "email/welcome-template").join();

        assertThat(resultado).isTrue();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void sendEmail_devuelveFalseYAuditaUnaVezCuandoElProveedorFalla() {
        doThrow(new MailSendException("Brevo no responde")).when(mailSender).send(any(MimeMessage.class));

        boolean resultado = emailService.sendEmail(mail(), "email/welcome-template").join();

        assertThat(resultado).isFalse();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(auditLogService, times(1)).log(
                eq("ENVIO_CORREO_FALLIDO"), eq("Correos"), contains("cliente@example.com"));
    }

    @Test
    void sendEmailWithRetry_noReintentaSiElPrimerIntentoYaTuvoExito() {
        boolean resultado = emailService.sendEmailWithRetry(mail(), "email/welcome-template").join();

        assertThat(resultado).isTrue();
        // Exactamente un envío real: un éxito nunca debe traducirse en más de un correo entregado.
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void sendEmailWithRetry_reintentaTrasUnaFallaYSeDetieneApenasTeneExito() {
        doThrow(new MailSendException("timeout"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        boolean resultado = emailService.sendEmailWithRetry(mail(), "email/welcome-template").join();

        assertThat(resultado).isTrue();
        // Un intento fallido + un intento exitoso = 2 llamadas reales al proveedor, nunca más
        // (no sigue reintentando después de haber tenido éxito).
        verify(mailSender, times(2)).send(any(MimeMessage.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void sendEmailWithRetry_agotaLosIntentosYAuditaUnaSolaVezSiSiempreFalla() {
        doThrow(new MailSendException("Brevo caído")).when(mailSender).send(any(MimeMessage.class));

        boolean resultado = emailService.sendEmailWithRetry(mail(), "email/welcome-template").join();

        assertThat(resultado).isFalse();
        verify(mailSender, times(3)).send(any(MimeMessage.class));
        // La auditoría registra el fallo terminal una sola vez, no una vez por intento.
        verify(auditLogService, times(1)).log(anyString(), anyString(), anyString());
    }
}
