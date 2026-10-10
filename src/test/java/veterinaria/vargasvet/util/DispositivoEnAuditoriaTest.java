package veterinaria.vargasvet.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DispositivoEnAuditoriaTest {

    @Test
    void laMigracionAgregaElDispositivoAlAvisoYALaAuditoriaYLoProtegeDeCambios() throws Exception {
        Path migracion = Path.of("src/main/resources/db/migration/V104__Dispositivo_En_Aviso_Y_Auditoria.sql");
        assumeTrue(Files.exists(migracion), "Las migraciones se aplican a mano y no se publican en el repositorio");
        String sql = Files.readString(migracion);

        assertThat(sql).contains("ALTER TABLE aviso_privacidad").contains("creado_dispositivo").contains("creado_ip")
                .contains("ALTER TABLE audit_logs").contains("dispositivo VARCHAR(120)")
                .contains("NEW.creado_dispositivo IS DISTINCT FROM OLD.creado_dispositivo")
                .contains("NEW.creado_ip IS DISTINCT FROM OLD.creado_ip")
                .contains("CREATE OR REPLACE FUNCTION aviso_privacidad_inmutable()");
    }
}
