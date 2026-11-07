package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TrazabilidadTecnicaMigrationTest {

    @Test
    void laMigracionCorrelacionaAuditoriaYProtegeLosLogsTecnicos() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V129__Trazabilidad_Tecnica_Y_Correlacion.sql"));

        assertThat(sql)
                .contains("ALTER TABLE audit_logs")
                .contains("correlation_id VARCHAR(100)")
                .contains("outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS'")
                .contains("CREATE TABLE IF NOT EXISTS system_logs")
                .contains("CHECK (outcome IN ('SUCCESS', 'FAILED', 'DENIED', 'UNKNOWN'))")
                .contains("CREATE TRIGGER trg_system_logs_no_update")
                .contains("CREATE TRIGGER trg_system_logs_no_delete")
                .contains("Los registros técnicos son inmutables")
                .contains("sin cuerpos, credenciales, tokens ni parámetros de consulta");
    }
}
