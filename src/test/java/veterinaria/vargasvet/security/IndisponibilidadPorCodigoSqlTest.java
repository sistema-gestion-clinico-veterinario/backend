package veterinaria.vargasvet.security;

import jakarta.persistence.PersistenceException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.hibernate.TransactionException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionSystemException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los errores que PostgreSQL devuelve sin ser de la clase 08 (por ejemplo 57P01 al apagar el servidor con
 * consultas en curso) pasan por las conversiones reales de Hibernate y de Spring, que es lo que llega a los filtros.
 */
class IndisponibilidadPorCodigoSqlTest {

    static RuntimeException comoLlegaAlFiltro(String sqlState) {
        SQLException sql = new SQLException("FATAL: error del servidor de base de datos", sqlState);
        var convertidor = new StandardSQLExceptionConverter(new SQLStateConversionDelegate(() -> error -> null));
        var hibernate = convertidor.convert(sql, "no se pudo ejecutar la consulta", "select 1");
        return new HibernateJpaDialect().translateExceptionIfPossible(new PersistenceException(hibernate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"57P01", "57P02", "57P03", "53300", "08006", "08003", "08001"})
    void reconoceLosCodigosDeServidorNoDisponible(String sqlState) {
        assertThat(IndisponibilidadDeBaseDeDatos.es(comoLlegaAlFiltro(sqlState))).as(sqlState).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"23505", "23503", "42P01", "42703", "40001", "22001", "57014"})
    void noConfundeErroresDeDatosNiDeProgramacionConUnaCaida(String sqlState) {
        assertThat(IndisponibilidadDeBaseDeDatos.es(comoLlegaAlFiltro(sqlState))).as(sqlState).isFalse();
    }

    @Test
    void reconoceElCodigoCuandoViajaEnLaExcepcionSiguienteDelDriver() {
        SQLException principal = new SQLException("error al ejecutar", "XX000");
        principal.setNextException(new SQLException("FATAL: terminating connection due to administrator command", "57P01"));

        assertThat(IndisponibilidadDeBaseDeDatos.es(new IllegalStateException("capa intermedia", principal))).isTrue();
    }

    @Test
    void elResumenListaLasClasesYElCodigoSqlSinMensajes() {
        SQLException sql = new SQLException("dato privado que no debe aparecer", "57P01");

        String resumen = IndisponibilidadDeBaseDeDatos.resumen(new IllegalStateException("otro dato privado", sql));

        assertThat(resumen).isEqualTo("IllegalStateException <- SQLException[57P01]");
        assertThat(IndisponibilidadDeBaseDeDatos.resumen(null)).isEmpty();
    }

    @Test
    void unRollbackQueFallaPorqueLaConexionYaEstaCerradaEsUnaCaida() {
        SQLException cerrada = new SQLException("Connection is closed");
        JpaSystemException comoLlegaAlFiltro = new JpaSystemException(
                new TransactionException("Unable to rollback against JDBC Connection", cerrada));

        assertThat(IndisponibilidadDeBaseDeDatos.es(comoLlegaAlFiltro)).isTrue();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new TransactionSystemException("Could not roll back JPA transaction",
                new RuntimeException(cerrada)))).isTrue();
    }

    @Test
    void unFalloAlConfirmarPorUnErrorDeDatosNoEsUnaCaida() {
        SQLException duplicada = new SQLException("clave duplicada", "23505");
        JpaSystemException comoLlegaAlFiltro = new JpaSystemException(
                new TransactionException("Unable to commit against JDBC Connection", duplicada));

        assertThat(IndisponibilidadDeBaseDeDatos.es(comoLlegaAlFiltro)).isFalse();
    }

    @Test
    void unaExcepcionSinCodigoSqlNoSeTomaPorCaida() {
        assertThat(IndisponibilidadDeBaseDeDatos.es(new SQLException("sin código"))).isFalse();
        assertThat(IndisponibilidadDeBaseDeDatos.es(new SQLException("con código vacío", ""))).isFalse();
    }
}
