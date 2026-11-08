package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TrazabilidadIntegralAtencionPagosMigrationTest {

    @Test
    void migrationAddsPersistentTraceIdempotencyAndReceiptRecoveryState() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V131__Trazabilidad_Integral_Atencion_Y_Pagos.sql"));

        assertThat(sql)
                .contains("business_trace_id")
                .contains("idempotency_key")
                .contains("receipt_status")
                .contains("previous_value")
                .contains("new_value")
                .contains("CREATE UNIQUE INDEX");
    }
}
