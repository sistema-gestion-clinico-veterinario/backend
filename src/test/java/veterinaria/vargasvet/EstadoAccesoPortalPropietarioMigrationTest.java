package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoAccesoPortalPropietarioMigrationTest {

    @Test
    void usaElNombreRealDeLaTablaDeRoles() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V134__estado_acceso_portal_propietario.sql"));

        assertThat(sql)
                .contains("JOIN roles r ON r.id = upr.rol_id")
                .doesNotContain("JOIN rol r ON r.id = upr.rol_id");
    }
}
