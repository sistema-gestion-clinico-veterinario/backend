package veterinaria.vargasvet.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service = new PasswordPolicyService();

    @Test
    void acceptsLongMemorablePassphraseWithoutForcedComposition() {
        assertDoesNotThrow(() -> service.validate(
                "Lago tranquilo con siete nubes", "persona@example.com", "Ana", "Rojas"));
    }

    @Test
    void rejectsShortPredictableAndPersonalPasswords() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate("Corta-123", "persona@example.com", "Ana", "Rojas"));
        assertThrows(IllegalArgumentException.class,
                () -> service.validate("qwerty123456789", "persona@example.com", "Ana", "Rojas"));
        assertThrows(IllegalArgumentException.class,
                () -> service.validate("persona-acceso-seguro", "persona@example.com", "Ana", "Rojas"));
    }

    @Test
    void rejectsValuesThatBcryptWouldSilentlyTruncate() {
        assertThrows(IllegalArgumentException.class,
                () -> service.validate("🐾".repeat(20), "persona@example.com", "Ana", "Rojas"));
    }
}
