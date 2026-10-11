package veterinaria.vargasvet.security;

import java.util.Locale;

/**
 * Construye la identidad lógica de una credencial para controles de abuso.
 *
 * <p>Una misma persona puede tener credenciales independientes en varias
 * empresas. Por ello, los contadores de intentos y los bloqueos nunca deben
 * usar únicamente el correo o nombre de usuario: hacerlo permitiría que un
 * intento en una clínica afecte el acceso a otra.</p>
 */
public final class AuthenticationAttemptKey {

    private AuthenticationAttemptKey() {
    }

    public static String forCompany(Integer companyId, String identifier) {
        if (companyId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para una credencial de clínica");
        }
        return "company:" + companyId + ":" + normalize(identifier);
    }

    public static String forCompanyAccount(Integer companyId, Integer userId) {
        if (companyId == null || userId == null) {
            throw new IllegalArgumentException("La empresa y la cuenta son obligatorias");
        }
        return "company:" + companyId + ":account:" + userId;
    }

    public static String forPlatform(String identifier) {
        return "platform:" + normalize(identifier);
    }

    public static String forPlatformAccount(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("La cuenta de plataforma es obligatoria");
        }
        return "platform:account:" + userId;
    }

    public static String forUnresolvedCompany(String slug, String identifier) {
        String companyReference = slug == null || slug.isBlank()
                ? "missing"
                : slug.trim().toLowerCase(Locale.ROOT);
        return "unresolved-company:" + companyReference + ":" + normalize(identifier);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
