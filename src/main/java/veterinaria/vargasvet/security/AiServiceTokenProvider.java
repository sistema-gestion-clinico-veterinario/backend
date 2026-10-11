package veterinaria.vargasvet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Emite una autorización técnica de una sola finalidad después de que el
 * controlador haya validado permiso, empresa, mascota y consentimiento.
 * No autentica nuevamente al usuario.
 */
@Component
@RequiredArgsConstructor
public class AiServiceTokenProvider {

    private final TokenProvider tokenProvider;

    public String issue(AiServiceScope scope, Long mascotaId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Integer companyId = SecurityUtils.getCurrentCompanyId();
        if (userId == null || companyId == null) {
            throw new IllegalStateException("No existe un contexto autenticado de clínica para usar IA");
        }
        if (scope == null || mascotaId == null) {
            throw new IllegalArgumentException("La operación y la mascota son obligatorias");
        }
        return tokenProvider.createAiServiceToken(userId, companyId, scope.value(), mascotaId);
    }
}
