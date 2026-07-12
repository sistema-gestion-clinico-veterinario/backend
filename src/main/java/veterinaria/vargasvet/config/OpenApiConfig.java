package veterinaria.vargasvet.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI vargasVetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema Multimodal para la Asistencia en el diagnóstico clínico veterinario")
                        .version("1.0.0")
                        .description("""
                                Documentación de la API REST del sistema para la gestión clínica veterinaria,
                                incluyendo autenticación, usuarios, empresas, pacientes, citas, historias clínicas,
                                servicios, pagos y funcionalidades de asistencia al diagnóstico.
                                """)
                        .contact(new Contact()
                                .name("Equipo de desarrollo Vargas Vet")
                                .email("soporte@vargasvet.pe"))
                        .license(new License()
                                .name("Uso académico")))
                .servers(List.of(
                        new Server()
                                .url("/api/v1")
                                .description("Contexto base de la API"),
                        new Server()
                                .url("https://backend-cwx3.onrender.com/api/v1")
                                .description("Backend desplegado en Render")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT con el formato: Bearer {token}")));
    }
}
