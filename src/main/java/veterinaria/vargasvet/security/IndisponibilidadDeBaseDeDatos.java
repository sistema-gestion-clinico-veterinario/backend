package veterinaria.vargasvet.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.Set;

public final class IndisponibilidadDeBaseDeDatos {

    public static final int REINTENTAR_EN_SEGUNDOS = 5;
    public static final String MENSAJE = "El servicio no está disponible temporalmente. Intenta de nuevo en unos segundos.";

    private static final int PROFUNDIDAD_MAXIMA = 10;
    private static final String CLASE_ERROR_DE_CONEXION = "08";
    private static final Set<String> ESTADOS_DE_SERVIDOR_NO_DISPONIBLE = Set.of("57P01", "57P02", "57P03", "53300");

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
                    || actual instanceof SQLRecoverableException
                    || actual instanceof SQLException sql && indicaServidorNoDisponible(sql)) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private static boolean indicaServidorNoDisponible(SQLException error) {
        SQLException actual = error;
        for (int paso = 0; actual != null && paso < PROFUNDIDAD_MAXIMA; paso++) {
            String estado = actual.getSQLState();
            if (estado != null && (estado.startsWith(CLASE_ERROR_DE_CONEXION) || ESTADOS_DE_SERVIDOR_NO_DISPONIBLE.contains(estado))) {
                return true;
            }
            actual = actual.getNextException();
        }
        return false;
    }

    public static String resumen(Throwable error) {
        StringBuilder cadena = new StringBuilder();
        Throwable actual = error;
        for (int nivel = 0; actual != null && nivel < PROFUNDIDAD_MAXIMA; nivel++) {
            if (nivel > 0) {
                cadena.append(" <- ");
            }
            cadena.append(actual.getClass().getSimpleName());
            if (actual instanceof SQLException sql && sql.getSQLState() != null) {
                cadena.append('[').append(sql.getSQLState()).append(']');
            }
            actual = actual.getCause();
        }
        return cadena.toString();
    }

    public static void responder(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", String.valueOf(REINTENTAR_EN_SEGUNDOS));
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + MENSAJE + "\",\"data\":null}");
    }
}
