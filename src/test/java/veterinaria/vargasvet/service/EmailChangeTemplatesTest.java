package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Las plantillas del cambio de correo muestran "No fui yo" solo cuando hay un enlace para cancelar. */
class EmailChangeTemplatesTest {

    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    private Map<String, Object> model(String type, boolean administrative, String cancelUrl) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", "Ana Pérez");
        model.put("newEmail", "nuevo@example.com");
        model.put("companyName", "Vargas Vet");
        model.put("confirmationType", type);
        model.put("administrative", administrative);
        model.put("confirmationUrl", "https://app.test/vargas-vet/confirm-email-change#type=" + type + "&token=abc");
        if (cancelUrl != null) {
            model.put("cancelUrl", cancelUrl);
        }
        return model;
    }

    private String render(String template, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        return engine.process(template, context);
    }

    @Test
    void elCorreoActualMuestraElEnlaceParaCancelarYLaExplicacionDelDobleConfirmacion() {
        String html = render("email/email-change-confirmation-template",
                model("actual", false, "https://app.test/vargas-vet/confirm-email-change#type=cancelar&token=abc"));

        assertThat(html).contains("No fui yo — cancelar esta solicitud")
                .contains("type=cancelar&amp;token=abc")
                .contains("ambos correos hayan sido confirmados")
                .doesNotContain("Un administrador de la empresa solicitó este cambio");
    }

    @Test
    void elCorreoNuevoNoOfreceCancelar() {
        String html = render("email/email-change-confirmation-template", model("nuevo", false, null));

        assertThat(html).doesNotContain("No fui yo").contains("Confirma que tienes acceso al nuevo correo");
    }

    @Test
    void laConfirmacionDeUnCambioAdministrativoExplicaQueLoSolicitoUnAdministrador() {
        String html = render("email/email-change-confirmation-template", model("nuevo", true, null));

        assertThat(html).contains("Un administrador de la empresa solicitó este cambio")
                .doesNotContain("ambos correos hayan sido confirmados");
    }

    @Test
    void elAvisoAlCorreoAnteriorDelCambioAdministrativoTieneElBotonParaCancelar() {
        Map<String, Object> model = model("aviso", true,
                "https://app.test/vargas-vet/confirm-email-change#type=cancelar&token=zzz");
        model.put("validityHours", 24L);

        String html = render("email/email-change-admin-notice-template", model);

        assertThat(html).contains("No fui yo — cancelar solicitud")
                .contains("type=cancelar&amp;token=zzz")
                .contains("24");
    }
}
