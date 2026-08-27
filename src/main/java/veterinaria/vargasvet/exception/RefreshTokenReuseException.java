package veterinaria.vargasvet.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class RefreshTokenReuseException extends BadCredentialsException {
    public RefreshTokenReuseException() {
        super("Se detectó la reutilización de una sesión revocada");
    }
}
