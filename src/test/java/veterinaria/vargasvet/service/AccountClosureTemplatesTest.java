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

class AccountClosureTemplatesTest {

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

    private String render(String template, Map<String, Object> extra) {
        Map<String, Object> model = new HashMap<>();
        model.put("nombre", "Ana Pérez");
        model.put("companyName", "Vargas Vet");
        model.putAll(extra);
        Context context = new Context();
        context.setVariables(model);
        return engine.process(template, context);
    }

    @Test
    void elCorreoDelCodigoMuestraElCodigoSuVigenciaYQueSePuedeIgnorar() {
        String html = render("email/account-close-code-template", Map.of("code", "482913", "validityMinutes", 10));

        assertThat(html).contains("482913").contains("10</span> minutos").contains("Si no pediste cerrar tu cuenta")
                .contains("Ana Pérez").contains("Vargas Vet");
    }

    @Test
    void elCorreoDeCierreExplicaQueSeConservaYComoReactivar() {
        String html = render("email/account-closed-template", Map.of(
                "graceDays", 30, "expiresOn", "04/11/2026",
                "reactivateUrl", "https://app.test/vargas-vet/reactivate-account#token=abc"));

        assertThat(html).contains("Cerraste tu cuenta").contains("04/11/2026").contains("30</span> días")
                .contains("https://app.test/vargas-vet/reactivate-account#token=abc")
                .contains("conserva sus registros").contains("se eliminarán tus credenciales");
    }

    @Test
    void elCorreoDeReactivacionLlevaAlLogin() {
        String html = render("email/account-reactivated-template", Map.of("loginUrl", "https://app.test/vargas-vet/login"));

        assertThat(html).contains("Tu cuenta está activa de nuevo").contains("https://app.test/vargas-vet/login");
    }
}
