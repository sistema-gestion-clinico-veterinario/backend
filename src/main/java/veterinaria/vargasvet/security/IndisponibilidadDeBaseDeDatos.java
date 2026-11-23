package veterinaria.vargasvet.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;

import java.io.IOException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

public final class IndisponibilidadDeBaseDeDatos {

    public static final int REINTENTAR_EN_SEGUNDOS = 5;
    public static final String MENSAJE = "El servicio no está disponible temporalmente. Intenta de nuevo en unos segundos.";

    private static final int PROFUNDIDAD_MAXIMA = 10;

    private IndisponibilidadDeBaseDeDatos() {
    }

    public static boolean es(Throwable error) {
        Throwable actual = error;
        for (int nivel = 0; actual != null && nivel < PROFUNDIDAD_MAXIMA; nivel++) {
            if (actual instanceof DataAccessResourceFailureException
                    || actual instanceof CannotCreateTransactionException
                    || actual instanceof QueryTimeoutException
                    || actual instanceof TransactionTimedOutException
                    || actual instanceof org.hibernate.exception.JDBCConnectionException
                    || actual instanceof SQLTransientConnectionException
                    || actual instanceof SQLNonTransientConnectionException
                    || actual instanceof SQLRecoverableException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    public static void responder(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", String.valueOf(REINTENTAR_EN_SEGUNDOS));
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + MENSAJE + "\",\"data\":null}");
    }
}
