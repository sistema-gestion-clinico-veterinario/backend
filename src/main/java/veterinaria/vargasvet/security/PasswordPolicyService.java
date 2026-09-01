package veterinaria.vargasvet.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 12;
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password123", "contrasena", "contraseña", "qwerty123",
            "12345678", "123456789", "administrador", "admin123", "veterinaria",
            "bienvenido", "welcome123", "letmein", "iloveyou"
    );

    public void validate(String password, String email, String firstName, String lastName) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new IllegalArgumentException("La contraseña es demasiado larga para almacenarse de forma segura");
        }

        String normalizedPassword = normalize(password);
        if (COMMON_PASSWORDS.contains(normalizedPassword)
                || normalizedPassword.chars().distinct().count() < 4
                || normalizedPassword.matches(".*(012345|123456|234567|345678|456789|abcdef|qwerty).*")) {
            throw new IllegalArgumentException("La contraseña es demasiado común o predecible");
        }

        rejectIdentityFragment(normalizedPassword, email == null ? null : email.split("@", 2)[0]);
        rejectIdentityFragment(normalizedPassword, firstName);
        rejectIdentityFragment(normalizedPassword, lastName);
    }

    private void rejectIdentityFragment(String password, String value) {
        String fragment = normalize(value);
        if (fragment.length() >= 4 && password.contains(fragment)) {
            throw new IllegalArgumentException("La contraseña no debe contener datos personales o el correo");
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
