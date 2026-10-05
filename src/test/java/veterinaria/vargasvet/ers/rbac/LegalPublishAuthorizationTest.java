package veterinaria.vargasvet.ers.rbac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import veterinaria.vargasvet.controller.LegalController;
import veterinaria.vargasvet.domain.enums.LegalDocumentType;
import veterinaria.vargasvet.domain.enums.RolePurpose;
import veterinaria.vargasvet.domain.enums.RoleScope;
import veterinaria.vargasvet.dto.request.PublishLegalDocumentRequest;
import veterinaria.vargasvet.security.AccesoValidator;
import veterinaria.vargasvet.security.ClientIpResolver;
import veterinaria.vargasvet.security.RolePermissionEvaluator;
import veterinaria.vargasvet.security.UsuarioPrincipal;
import veterinaria.vargasvet.service.LegalDocumentService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Con la seguridad por método activa de verdad: publicar un documento legal obliga a todas las personas a
 * aceptarlo de nuevo, así que solo el administrador de plataforma puede hacerlo.
 */
@SpringJUnitConfig(LegalPublishAuthorizationTest.Config.class)
class LegalPublishAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        LegalDocumentService legalDocumentService() {
            return mock(LegalDocumentService.class);
        }

        @Bean
        ClientIpResolver clientIpResolver() {
            return mock(ClientIpResolver.class);
        }

        @Bean
        AccesoValidator accesoValidator() {
            return new AccesoValidator(mock(RolePermissionEvaluator.class));
        }

        @Bean
        LegalController legalController(LegalDocumentService service, ClientIpResolver ipResolver) {
            return new LegalController(service, ipResolver);
        }
    }

    @Autowired private LegalController controller;
    @Autowired private LegalDocumentService legalDocumentService;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        reset(legalDocumentService);
    }

    private void autenticarCon(RolePurpose purpose) {
        UsuarioPrincipal principal = new UsuarioPrincipal(1, "persona@vargasvet.test", "", List.of(), 7,
                purpose == null ? null : 5, purpose == RolePurpose.PLATFORM_ADMIN ? RoleScope.PLATFORM : RoleScope.STAFF,
                purpose, 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private PublishLegalDocumentRequest solicitud() {
        PublishLegalDocumentRequest request = new PublishLegalDocumentRequest();
        request.setTipo(LegalDocumentType.TERMINOS_Y_CONDICIONES);
        request.setVersion("2.0");
        request.setContenido("Texto definitivo");
        return request;
    }

    @Test
    @DisplayName("[CP-RF-AUT-20] Solo el administrador de plataforma puede publicar un documento legal")
    void soloElAdministradorDePlataformaPublica() {
        autenticarCon(RolePurpose.PLATFORM_ADMIN);

        var respuesta = controller.publish(solicitud());

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        verify(legalDocumentService).publish(LegalDocumentType.TERMINOS_Y_CONDICIONES, "2.0", "Texto definitivo");
    }

    @Test
    @DisplayName("[CP-RF-AUT-20] Un administrador de clínica, un rol personalizado o un cliente no pueden publicar")
    void nadieMasPuedePublicar() {
        for (RolePurpose purpose : new RolePurpose[]{RolePurpose.COMPANY_ADMIN, RolePurpose.CUSTOM,
                RolePurpose.CLIENT_PORTAL, null}) {
            autenticarCon(purpose);

            assertThatThrownBy(() -> controller.publish(solicitud()))
                    .as("propósito %s", purpose)
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        verify(legalDocumentService, never()).publish(any(), any(), any());
    }

    @Test
    @DisplayName("[CP-RF-AUT-20] Sin sesión no se publica nada")
    void sinSesionNoSePublica() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.publish(solicitud()))
                .isInstanceOfAny(org.springframework.security.access.AccessDeniedException.class,
                        org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
        verify(legalDocumentService, never()).publish(any(), any(), any());
    }
}
