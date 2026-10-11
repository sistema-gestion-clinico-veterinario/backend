package veterinaria.vargasvet.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class SessionInactivityException extends BadCredentialsException {

    public static final String CODE = "SESSION_IDLE_TIMEOUT";

    public SessionInactivityException() {
        super("Tu sesión se cerró por inactividad. Inicia sesión nuevamente.");
    }
}
