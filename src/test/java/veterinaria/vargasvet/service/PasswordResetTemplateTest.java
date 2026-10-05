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

/** El correo de restablecimiento dice quién lo originó: la propia persona o un administrador. */
class PasswordResetTemplateTest {

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

    private String render(Boolean initiatedByAdmin) {
        Map<String, Object> model = new HashMap<>();
        model.put("usuario", "Ana Pérez");
        model.put("companyName", "Vargas Vet");
        model.put("companyEmail", "contacto@vargasvet.test");
        model.put("resetUrl", "https://app.test/vargas-vet/reset-password#token=abc");
        model.put("currentYear", 2026);
        if (initiatedByAdmin != null) {
            model.put("initiatedByAdmin", initiatedByAdmin);
        }
        Context context = new Context();
        context.setVariables(model);
        return engine.process("email/forgot-password-template", context);
    }

    @Test
    void elRestablecimientoPedidoPorUnAdministradorLoDiceYNoPidePerdonarElCambioPropio() {
        String html = render(true);

        assertThat(html).contains("Un administrador solicitó restablecer la contraseña")
                .contains("consulta con tu administrador")
                .doesNotContain("Hemos recibido una solicitud")
                .doesNotContain("Si no fuiste tú quien solicitó");
    }

    @Test
    void laRecuperacionPropiaConservaSuTexto() {
        for (Boolean valor : new Boolean[]{false, null}) {
            String html = render(valor);

            assertThat(html).contains("Hemos recibido una solicitud para restablecer")
                    .contains("Si no fuiste tú quien solicitó")
                    .doesNotContain("Un administrador solicitó");
        }
    }
}
