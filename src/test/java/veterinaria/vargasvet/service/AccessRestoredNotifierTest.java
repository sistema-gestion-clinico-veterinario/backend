package veterinaria.vargasvet.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import veterinaria.vargasvet.domain.entity.Company;
import veterinaria.vargasvet.domain.entity.Usuario;
import veterinaria.vargasvet.dto.Mail;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRestoredNotifierTest {

    @Mock EmailService emailService;
    @InjectMocks AccessRestoredNotifier notifier;

    private Company company;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifier, "frontendUrl", "https://app.test");
        company = new Company();
        company.setName("Vargas Vet");
        company.setSlug("vargas-vet");
        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setEmail("ana@example.test");
        usuario.setEmailVerified(true);
    }

    @AfterEach
    void limpiar() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void avisaALaPersonaConElLoginDeSuClinica() {
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        when(emailService.createMail(eq("ana@example.test"), anyString(), modelo.capture())).thenReturn(new Mail());

        notifier.send(usuario, company);

        verify(emailService).createMail(eq("ana@example.test"), eq("Tu acceso a Vargas Vet fue restablecido"), any());
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/access-restored-template"));
        assertThat(modelo.getValue()).containsEntry("nombre", "Ana Pérez").containsEntry("companyName", "Vargas Vet");
        assertThat(String.valueOf(modelo.getValue().get("loginUrl"))).startsWith("https://app.test").contains("vargas-vet");
    }

    @Test
    void noAvisaAQuienTodaviaNoActivoSuCuenta() {
        usuario.setEmailVerified(false);

        notifier.send(usuario, company);

        verifyNoInteractions(emailService);
    }

    @Test
    void siElCorreoFallaLaReactivacionNoSeEntera() {
        when(emailService.createMail(anyString(), anyString(), any())).thenThrow(new IllegalStateException("plantilla caída"));

        notifier.send(usuario, company);

        verify(emailService, never()).sendEmailWithRetry(any(), anyString());
    }

    @Test
    void dentroDeUnaTransaccionEsperaAQueSeConfirme() {
        when(emailService.createMail(anyString(), anyString(), any())).thenReturn(new Mail());
        TransactionSynchronizationManager.initSynchronization();

        notifier.send(usuario, company);

        verifyNoInteractions(emailService);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(emailService).sendEmailWithRetry(any(Mail.class), eq("email/access-restored-template"));
    }

    @Test
    void siLaTransaccionSeRevierteNoSeEnviaNada() {
        TransactionSynchronizationManager.initSynchronization();

        notifier.send(usuario, company);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(emailService);
    }

    @Test
    void laPlantillaInvitaAIniciarSesionYPideAvisarSiNoLaEsperaba() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariables(Map.of("nombre", "Ana Pérez", "companyName", "Vargas Vet",
                "loginUrl", "https://app.test/vargas-vet/login"));

        String html = engine.process("email/access-restored-template", context);

        assertThat(html).contains("Tu acceso fue restablecido").contains("Ana Pérez").contains("Vargas Vet")
                .contains("https://app.test/vargas-vet/login").contains("Si no esperabas este aviso");
    }
}
