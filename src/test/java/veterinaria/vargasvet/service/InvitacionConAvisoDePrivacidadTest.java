package veterinaria.vargasvet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvitacionConAvisoDePrivacidadTest {

    private SpringTemplateEngine motor;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolver);
    }

    private String render(String plantilla, Map<String, Object> modelo) {
        Context contexto = new Context();
        contexto.setVariables(modelo);
        return motor.process("email/" + plantilla, contexto);
    }

    private Map<String, Object> modelo(String avisoLink) {
        java.util.HashMap<String, Object> modelo = new java.util.HashMap<>();
        modelo.put("nombre", "Ana");
        modelo.put("companyName", "Clínica Patitas");
        modelo.put("verificationLink", "https://app.test/patitas/auth/verify#token=abc");
        modelo.put("validityHours", 24);
        modelo.put("avisoPrivacidadLink", avisoLink);
        return modelo;
    }

    @Test
    void laInvitacionDelPersonalIncluyeElEnlaceAlAvisoDeLaClinica() {
        String html = render("welcome-template", modelo("https://app.test/patitas/privacidad"));

        assertThat(html).contains("https://app.test/patitas/privacidad").contains("aviso de privacidad")
                .contains("Clínica Patitas").contains("https://app.test/patitas/auth/verify#token=abc");
    }

    @Test
    void laInvitacionDelClienteIncluyeElEnlaceAlAvisoDeLaClinica() {
        String html = render("welcome-apoderado-template", modelo("https://app.test/patitas/privacidad"));

        assertThat(html).contains("https://app.test/patitas/privacidad").contains("aviso de privacidad")
                .contains("https://app.test/patitas/auth/verify#token=abc");
    }

    @Test
    void sinEnlaceLaFraseNoApareceEnNingunaDeLasDos() {
        assertThat(render("welcome-template", modelo(null))).doesNotContain("aviso de privacidad");
        assertThat(render("welcome-apoderado-template", modelo(null))).doesNotContain("aviso de privacidad");
    }
}
