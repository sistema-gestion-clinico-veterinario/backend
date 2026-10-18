package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VersionesBloqueoOptimistaMigrationTest {

    @Test
    void laMigracionReparaVersionesNulasYEvitaQueReaparezcan() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V119__Normalizar_Versiones_Bloqueo_Optimista.sql"));

        for (String tabla : new String[]{"citas", "consulta", "control_preventivo",
                "registro_vacunas", "registro_desparasitaciones"}) {
            assertThat(sql).contains("UPDATE " + tabla + " SET version = 0 WHERE version IS NULL")
                    .contains("ALTER TABLE " + tabla + " ALTER COLUMN version SET DEFAULT 0")
                    .contains("ALTER TABLE " + tabla + " ALTER COLUMN version SET NOT NULL");
        }
    }
}
