package veterinaria.vargasvet.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcRepositorioEstadoOAuth implements RepositorioEstadoOAuth {

    private static final String RETIRAR_SQL = """
            DELETE FROM oauth_google_estado
             WHERE tipo = ? AND clave_hash = ?
            RETURNING contenido, (expira_en > CURRENT_TIMESTAMP) AS vigente
            """;

    private final JdbcTemplate jdbc;

    @Override
    public void purgarVencidos() {
        jdbc.update("DELETE FROM oauth_google_estado WHERE expira_en < CURRENT_TIMESTAMP");
    }

    @Override
    public int contar(String tipo) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM oauth_google_estado WHERE tipo = ?", Integer.class, tipo);
        return total == null ? 0 : total;
    }

    @Override
    public void insertar(String tipo, String claveHash, String contenido, long vidaSegundos) {
        jdbc.update("""
                INSERT INTO oauth_google_estado (tipo, clave_hash, contenido, expira_en)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP + make_interval(secs => ?))
                """, tipo, claveHash, contenido, (double) vidaSegundos);
    }

    @Override
    public Optional<Retirado> retirar(String tipo, String claveHash) {
        List<Retirado> filas = jdbc.query(RETIRAR_SQL,
                (rs, n) -> new Retirado(rs.getString("contenido"), rs.getBoolean("vigente")), tipo, claveHash);
        return filas.stream().findFirst();
    }
}
